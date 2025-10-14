package ru.antonov.oauth2_social.user.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ru.antonov.oauth2_social.auth.entity.TokenMode;
import ru.antonov.oauth2_social.auth.entity.TokenType;
import ru.antonov.oauth2_social.auth.exception.TokenUserMismatchEx;
import ru.antonov.oauth2_social.auth.service.JwtService;
import ru.antonov.oauth2_social.auth.service.TokenService;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.user.dto.DtoFactory;
import ru.antonov.oauth2_social.user.dto.UserCreateRequestDto;
import ru.antonov.oauth2_social.user.dto.UserResponseDto;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;
import ru.antonov.oauth2_social.user.entity.EntityFactory;
import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.exception.AccountActivationTokenRenewEx;
import ru.antonov.oauth2_social.user.exception.EmptyFileEx;
import ru.antonov.oauth2_social.user.service.EmailService;
import ru.antonov.oauth2_social.user.service.GroupService;
import ru.antonov.oauth2_social.user.service.PasswordGenerator;
import ru.antonov.oauth2_social.user.service.UserService;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;

import ru.antonov.oauth2_social.exception.AccessDeniedEx;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;
    private final GroupService groupService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AccessManager accessManager;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    private final Integer PASSWORD_LENGTH = 6;
    private final List<CharacterRule> PASSWORD_GEN_RULES = List.of(new CharacterRule(EnglishCharacterData.Digit));

    @PostMapping("/csv/institution")
    public ResponseEntity<List<UserResponseDto>> addUsersCSVByInstitutionId(
            @AuthenticationPrincipal User principal,
            @RequestParam("file") MultipartFile file
    ) {
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        if (file.isEmpty()) {
            throw new EmptyFileEx(
                    "Вы пытаетесь загрузить пустой файл.",
                    String.format("Ошибка при добавлении списка пользоваетелей. Пользователь %s попытался загрузить пустой файл %s",
                            principal.getEmail(), file.getOriginalFilename()
                    )
            );
        }

        List<User> addedUsers = userService.saveAllCSV(file, principal.getInstitution().getId(), null);
        addedUsers.forEach(this::sendMailForAccountActivation);

        return ResponseEntity.ok(
                addedUsers.stream()
                        .map(DtoFactory::makeUserResponseDto)
                        .toList()
        );

    }

    @PostMapping("/csv/group")
    public ResponseEntity<List<UserResponseDto>> addUsersCSVByGroupId(
            @AuthenticationPrincipal User principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("group_id") UUID groupId
    ) {
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Группы с указанным id не существует",
                        String.format("Неудача при поиске группы. Группа с %s id не существует", groupId)
                ));

        checkUserHasAccessToInstitutionOrElseThrow(principal, group.getInstitution().getId());

        if (file.isEmpty()) {
            throw new EmptyFileEx(
                    "Вы пытаетесь загрузить пустой файл.",
                    String.format("Ошибка при добавлении списка пользоваетелей. Пользователь %s попытался загрузить пустой файл %s",
                            principal.getEmail(), file.getOriginalFilename()
                    )
            );
        }

        List<User> addedUsers = userService.saveAllCSV(file, principal.getInstitution().getId(), groupId);
        addedUsers.forEach(this::sendMailForAccountActivation);

        return ResponseEntity.ok(
                addedUsers.stream()
                        .map(DtoFactory::makeUserResponseDto)
                        .toList()
        );

    }

    @PostMapping
    public ResponseEntity<UserResponseDto> addUser(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody UserCreateRequestDto request
    ){
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        Group group = groupService.findByInstitutionIdAndName(principal.getInstitution().getId(), request.getGroupName())
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Группы с указанным названием не существует",
                        String.format("Неудача при поиске группы. Группа с названием %s в учебном заведении %s не" +
                                " существует", request.getGroupName(), principal.getInstitution().getId())
                ));

        User user = EntityFactory.makeUserEntity(
                request,
                passwordEncoder.encode(passwordGenerator.generateRawPassword(PASSWORD_LENGTH, PASSWORD_GEN_RULES)),
                principal.getInstitution(),
                group
        );

        user = userService.save(user);

        sendMailForAccountActivation(user);

        return ResponseEntity.ok(
                DtoFactory.makeUserResponseDto(userService.save(user))
        );
    }

    @PostMapping("/batch")
    public ResponseEntity<List<UserResponseDto>> addUsersBatch(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody List<UserCreateRequestDto> requestList
    ) {
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        List<User> userEntities = requestList.stream()
                .map(dto -> {
                            Group group = groupService.findByInstitutionIdAndName(principal.getInstitution().getId(), dto.getGroupName())
                                    .orElseThrow(() -> new EntityNotFoundEx(
                                            String.format("Группы %s в данном учебном заведение не существует", dto.getGroupName()),
                                            String.format("Ошибка при добавлении списка пользователей. " +
                                                            "Группы %s в учебном заведении %s не существует",
                                                    dto.getGroupName(), principal.getInstitution().getId())
                                    ));
                            return EntityFactory.makeUserEntity(
                                    dto,
                                    passwordEncoder.encode(
                                            passwordGenerator.generateRawPassword(PASSWORD_LENGTH, PASSWORD_GEN_RULES)
                                    ),
                                    principal.getInstitution(),
                                    group
                            );
                        }
                )
                .toList();

        List<User> addedUsers = userService.saveAll(userEntities);
        addedUsers.forEach(this::sendMailForAccountActivation);

        return ResponseEntity.ok(
                addedUsers.stream()
                        .map(DtoFactory::makeUserResponseDto)
                        .toList()
        );
    }

    @GetMapping("/id")
    public ResponseEntity<UserResponseDto> findUserById(
            @AuthenticationPrincipal User principal,
            @RequestParam("user_id") UUID userId
    ) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Пользователя с %s id не существует", userId),
                        String.format("Ошибка при поиске пользователя. Пользователя с %s id не существует", userId)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, true, true);

        return ResponseEntity.ok(DtoFactory.makeUserResponseDto(user));
    }

    @GetMapping("/email")
    public ResponseEntity<UserResponseDto> findUserByEmail(
            @AuthenticationPrincipal User principal,
            @RequestParam("user_email") String email
    ) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Пользователя с %s email не существует", email),
                        String.format("Ошибка при поиске пользователя. Пользователя с email %s не существует", email)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, true, true);

        return ResponseEntity.ok(DtoFactory.makeUserResponseDto(user));
    }

    @GetMapping("/group")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByInstitutionIdAndGroup(
            @AuthenticationPrincipal User principal,
            @RequestParam("institution_id") UUID institutionId,
            @RequestParam("group") String group
    ) {
        checkUserHasAccessToInstitutionOrElseThrow(principal, institutionId);

        List<User> users = userService.findAllByInstitutionIdAndGroup(institutionId, group);

        return ResponseEntity.ok(
                users.stream()
                        .map(DtoFactory::makeUserShortResponseDto)
                        .toList()
        );
    }

    @GetMapping("/course")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByCourseId(
            @AuthenticationPrincipal User principal,
            @RequestParam("course_id") UUID courseId
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        List<User> users = userService.findAllByCourseId(courseId);

        return ResponseEntity.ok(
                users.stream()
                        .map(DtoFactory::makeUserShortResponseDto)
                        .toList()
        );
    }

    @GetMapping("/course/not")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByCourseIdNot(
            @AuthenticationPrincipal User principal,
            @RequestParam("institution_id") UUID institutionId,
            @RequestParam("course_id") UUID courseId
    ) {
        checkUserHasAccessToInstitutionOrElseThrow(principal, institutionId);

        List<User> users = userService.findAllByInstitutionIdNotInCourse(institutionId, courseId);

        return ResponseEntity.ok(
                users.stream()
                        .map(DtoFactory::makeUserShortResponseDto)
                        .toList()
        );
    }

    @GetMapping("/institution")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByInstitutionId(
            @AuthenticationPrincipal User principal,
            @RequestParam("institution_id") UUID institutionId
    ) {
        checkUserHasAccessToInstitutionOrElseThrow(principal, institutionId);

        List<User> users = userService.findAllByInstitutionId(institutionId);

        return ResponseEntity.ok(
                users.stream()
                        .map(DtoFactory::makeUserShortResponseDto)
                        .toList()
        );
    }

    @DeleteMapping("/id")
    public ResponseEntity<?> deleteUserById(
            @AuthenticationPrincipal User principal,
            @RequestParam("user_id") UUID userId
    ) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Пользователя с id %s не существует", userId)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, true, true);

        userService.deleteById(userId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/email")
    public ResponseEntity<?> deleteUserByEmail(
            @AuthenticationPrincipal User principal,
            @RequestParam("user_email") String email
    ) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Пользователя с email %s не существует", email)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, true, true);

        userService.deleteByEmail(email);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/activate-account")
    public ResponseEntity<String> activateAccount(
            @RequestParam("token") String accountActivationToken, @RequestParam("user_email") String userEmail
    ){
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь не найден",
                        String.format("Ошибка активации аккаунта. Пользователь с %s email не найден", userEmail)
                ));

        User tokenUser = tokenService.findUserByToken(accountActivationToken)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь не найден",
                        String.format("Ошибка активации аккаунта. Пользователь по токену %s не найден", accountActivationToken)
                ));

        if (!tokenUser.equals(user)) {
            throw new TokenUserMismatchEx(
                    "Данный токен принадлежит другому пользователю.",
                    String.format("Ошибка активации аккаунта. Несовпадение: токен принадлежит пользователю %s." +
                                    " Заявленный пользователь: %s",
                            tokenUser.getEmail(), userEmail
                    )
            );
        }

        if (!jwtService.isTokenValid(accountActivationToken, TokenMode.ACCOUNT_ACTIVATION)) {
            sendMailForAccountActivation(user);
            throw new AccountActivationTokenRenewEx(
                    "Время действия ссылки истекло. Вы получите новую ссылку на ваш email",
                    String.format("Неуспешная активация аккаунта. account_activation_token пользователя %s истек. " +
                            "Произошла выдача нового токена", userEmail)
            );
        }

        userService.enableAndSave(user);

        tokenService.revokeUserTokensByTokenModeIn(userEmail, List.of(TokenMode.ACCOUNT_ACTIVATION));

        return ResponseEntity.ok("Вы успешно активировали свой аккаунт!");
    }

    private void checkPrincipalIsAttachedToInstitutionOrElseThrow(User principal) {
        if (principal.getInstitution() == null) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не привязаны к учебному заведению.",
                    String.format("Отказ в доступе при добавлении списка пользователей. Пользователь %s не привязан к учебному заведению",
                            principal.getEmail())
            );
        }
    }

    private void checkUserHasAccessToOtherOrElseThrow(User user, User other, boolean isNeedToHaveHigherPriority,
                                                      boolean isNeedToBeInOneInstitute) {
        if (Objects.equals(user.getId(), other.getId())) {
            return;
        }

        if (!accessManager.isUserHasAccessToOther(user, other, isNeedToHaveHigherPriority, isNeedToBeInOneInstitute)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому пользователю",
                    String.format("Пользователю %s не разрешен доступ к пользователю %s", user.getEmail(), other.getEmail())
            );
        }
    }

    private void checkUserHasAccessToInstitutionOrElseThrow(User user, UUID institutionId) {
        if (!accessManager.isUserHasAccessToInstitution(user, institutionId)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому учебному заведению.",
                    String.format("Пользователю %s не разрешен доступ к учебному заведению %s", user.getEmail(), institutionId)
            );
        }
    }

    private void checkPrincipalHasAccessToCourseOrElseThrow(
            User principal, UUID courseId, boolean isNeedToBeCreator, boolean isNeedToHaveHigherRoleThanStudent){
        if(!accessManager.isUserHasAccessToCourse(principal, courseId, isNeedToBeCreator, isNeedToHaveHigherRoleThanStudent)){
            throw new AccessDeniedEx(
                    String.format("Ошибка доступа. У вас нет доступа к курсу %s.", courseId),
                    String.format("Отказ в доступе к курсу. Пользователь %s не имеет доступа к курсу %s",
                            principal.getEmail(), courseId)
            );
        }
    }

    private void sendMailForAccountActivation(User user){
        String accActivationToken = jwtService.generateUserToken(
                List.of(user.getRole().name()), user.getEmail(), TokenMode.ACCOUNT_ACTIVATION
        );
        tokenService.saveToken(accActivationToken, TokenType.BEARER, TokenMode.ACCOUNT_ACTIVATION, user);

        emailService.sendMailForAccountActivation(user, accActivationToken);
    }

}
