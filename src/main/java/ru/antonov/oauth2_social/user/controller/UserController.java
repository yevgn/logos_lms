package ru.antonov.oauth2_social.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ru.antonov.oauth2_social.auth.entity.TokenMode;
import ru.antonov.oauth2_social.auth.entity.TokenType;
import ru.antonov.oauth2_social.auth.service.AuthEmailService;
import ru.antonov.oauth2_social.auth.service.JwtService;
import ru.antonov.oauth2_social.auth.service.TokenService;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.user.dto.DtoFactory;
import ru.antonov.oauth2_social.user.dto.InstitutionResponseDto;
import ru.antonov.oauth2_social.user.dto.UserCreateRequestDto;
import ru.antonov.oauth2_social.user.dto.UserResponseDto;

import ru.antonov.oauth2_social.user.entity.EntityFactory;
import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.exception.EmptyFileEx;
import ru.antonov.oauth2_social.user.service.GroupService;
import ru.antonov.oauth2_social.user.service.InstitutionService;
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
    private final InstitutionService institutionService;
    private final GroupService groupService;
    private final TokenService tokenService;
    private final AuthEmailService authEmailService;
    private final JwtService jwtService;
    private final AccessManager accessManager;
    private final PasswordGenerator passwordGenerator;


    private final Integer PASSWORD_LENGTH = 6;
    private final List<CharacterRule> PASSWORD_GEN_RULES = List.of(new CharacterRule(EnglishCharacterData.Digit));

    @Operation(
            summary = "Добавление пользователей в учебное заведение csv списком",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное добавление пользователей"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не привязаны к учебному заведению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "В списке некорректные/дублирующиеся данные, данные неправильного формата или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Вы пытаетесь загрузить пустой файл"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Один или несколько пользователей из файла уже существуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. Один или несколько указанных пользователей уже существуют"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при чтении файла",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping(value = "/csv/institution", consumes = "multipart/form-data")
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

    @Operation(
            summary = "Добавление пользователей в учебную группу csv списком",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное добавление пользователей"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не привязаны к учебному заведению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "В списке некорректные/дублирующиеся данные, данные неправильного формата или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Вы пытаетесь загрузить пустой файл"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Один или несколько пользователей из файла уже существуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. Один или несколько указанных пользователей уже существуют"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при чтении файла",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping(value = "/csv/group", consumes = "multipart/form-data")
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

    @Operation(
            summary = "Добавление пользователя",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное добавление пользователя"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не привязаны к учебному заведению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Неправильный формат email"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Пользователь уже существует",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. Пользователь с email test@gmail.com уже существует"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping
    public ResponseEntity<UserResponseDto> addUser(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody UserCreateRequestDto request
    ) {
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        User user;
        if (request.getRole().equals(Role.TUTOR)) {
            user = EntityFactory.makeUserEntity(
                    request,
                    "{noop}" + passwordGenerator.generateRawPassword(PASSWORD_LENGTH, PASSWORD_GEN_RULES),
                    principal.getInstitution(),
                    null
            );
        } else {
            Group group = groupService.findByInstitutionIdAndName(principal.getInstitution().getId(), request.getGroupName())
                    .orElseThrow(() -> new EntityNotFoundEx(
                            "Группы с указанным названием не существует",
                            String.format("Неудача при поиске группы. Группа с названием %s в учебном заведении %s не" +
                                    " существует", request.getGroupName(), principal.getInstitution().getId())
                    ));

            user = EntityFactory.makeUserEntity(
                    request,
                    "{noop}" + passwordGenerator.generateRawPassword(PASSWORD_LENGTH, PASSWORD_GEN_RULES),
                    principal.getInstitution(),
                    group
            );
        }

        user = userService.save(user);

        sendMailForAccountActivation(user);

        return ResponseEntity.ok(
                DtoFactory.makeUserResponseDto(user)
        );
    }

    @Operation(
            summary = "Добавление пользователей ",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное добавление пользователей"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не привязаны к учебному заведению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Неправильный формат email"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Один или несколько пользователей уже существуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. Пользователь с email test@gmail.com уже существует"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/batch")
    public ResponseEntity<List<UserResponseDto>> addUsersBatch(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody List<UserCreateRequestDto> requestList
    ) {
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        List<User> userEntities = requestList.stream()
                .map(dto -> {
                            if (dto.getRole().equals(Role.TUTOR)) {
                                return EntityFactory.makeUserEntity(
                                        dto,
                                        "{noop}" + passwordGenerator.generateRawPassword(PASSWORD_LENGTH, PASSWORD_GEN_RULES),
                                        principal.getInstitution(),
                                        null
                                );
                            } else {
                                Group group = groupService.findByInstitutionIdAndName(principal.getInstitution().getId(), dto.getGroupName())
                                        .orElseThrow(() -> new EntityNotFoundEx(
                                                String.format("Группы %s в данном учебном заведение не существует", dto.getGroupName()),
                                                String.format("Ошибка при добавлении списка пользователей. " +
                                                                "Группы %s в учебном заведении %s не существует",
                                                        dto.getGroupName(), principal.getInstitution().getId())
                                        ));
                                return EntityFactory.makeUserEntity(
                                        dto,
                                        "{noop}" + passwordGenerator.generateRawPassword(PASSWORD_LENGTH, PASSWORD_GEN_RULES),
                                        principal.getInstitution(),
                                        group
                                );
                            }
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

    @Operation(
            summary = "Получение информации о пользователе по его id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "параметр user_id отсутствует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/id")
    public ResponseEntity<UserResponseDto> findUserById(
            @AuthenticationPrincipal User principal,
            @Valid @NotNull(message = "поле user_id не может отсутствовать") @RequestParam("user_id") UUID userId
    ) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Пользователя с %s id не существует", userId),
                        String.format("Ошибка при поиске пользователя. Пользователя с %s id не существует", userId)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, true, true);

        UserResponseDto response = DtoFactory.makeUserResponseDto(user);
        if(!principal.getRole().equals(Role.ADMIN)){
            response.setPassword(null);
        }

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получение информации о пользователе по его email",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "параметр user_email отсутствует"
                                        }
                                    """)
                    ))
    }
    )
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

        UserResponseDto response = DtoFactory.makeUserResponseDto(user);
        if(!principal.getRole().equals(Role.ADMIN)){
            response.setPassword(null);
        }

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Удаление пользователя по id",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Пользователя с указанным id не существует"
                                        }
                                    """)
                    ))
    }
    )
    @DeleteMapping("/id")
    public ResponseEntity<?> deleteUserById(
            @AuthenticationPrincipal User principal,
            @RequestParam("user_id") UUID userId
    ) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователя с указанным id не существует",
                        String.format("Ошибка при удалении пользователя. Пользователя с id %s не существует", userId)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, true, true);

        userService.deleteById(userId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Удаление пользователя по email",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Пользователя с указанным email не существует"
                                        }
                                    """)
                    ))
    }
    )
    @DeleteMapping("/email")
    public ResponseEntity<?> deleteUserByEmail(
            @AuthenticationPrincipal User principal,
            @RequestParam("user_email") String email
    ) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователя с указанным email не существует",
                        String.format("Ошибка при удалении пользователя. Пользователя с email %s не существует", email)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, true, true);

        userService.deleteByEmail(email);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Получении информации об учебном заведении по id пользователя",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление учебными заведениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Пользователя с указанным id не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{userId}/institution")
    public ResponseEntity<InstitutionResponseDto> findInstitutionByUserId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID userId
    ){
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователя с указанным id не существует",
                        String.format("Ошибка при поиске учебного заведения по id пользователя. " +
                                "Пользователя с id %s не существует", userId)
                ));

        checkUserHasAccessToOtherOrElseThrow(principal, user, false, true);

        return ResponseEntity.ok(institutionService.findInstitutionByUserId(userId));
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
            User principal, UUID courseId, boolean isNeedToBeCreator, boolean isNeedToHaveHigherRoleThanStudent) {
        if (!accessManager.isUserHasAccessToCourse(principal, courseId, isNeedToBeCreator, isNeedToHaveHigherRoleThanStudent)) {
            throw new AccessDeniedEx(
                    String.format("Ошибка доступа. У вас нет доступа к курсу %s.", courseId),
                    String.format("Отказ в доступе к курсу. Пользователь %s не имеет доступа к курсу %s",
                            principal.getEmail(), courseId)
            );
        }
    }

    private void sendMailForAccountActivation(User user) {
        String accActivationToken = jwtService.generateUserToken(
                List.of(user.getRole().name()), user.getEmail(), TokenMode.ACCOUNT_ACTIVATION
        );
        tokenService.saveToken(accActivationToken, TokenType.BEARER, TokenMode.ACCOUNT_ACTIVATION, user);

        authEmailService.sendMailForAccountActivation(user, accActivationToken);
    }

}
