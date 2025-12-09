package ru.antonov.oauth2_social.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.entity.Institution;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.common.exception.InvalidFileDataFormatEx;
import ru.antonov.oauth2_social.user.repository.UserRepository;
import ru.antonov.oauth2_social.common.exception.DBConstraintViolationEx;
import ru.antonov.oauth2_social.common.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.common.exception.IOEx;

import java.io.IOException;

import java.sql.SQLException;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final CsvParser csvParser;
//    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    private final Integer PASSWORD_LENGTH = 6;
    private final List<CharacterRule> PASSWORD_GEN_RULES = List.of(new CharacterRule(EnglishCharacterData.Digit));

    public boolean checkUserExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAllByIdList(List<UUID> userIdList){
        return userRepository.findAllByIdList(userIdList);
    }


    public List<User> findAllByGroupIdList(List<UUID> groupIdList){
        return userRepository.findAllByGroupIdList(groupIdList);
    }

    public User enableAndSave(User user){
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    public User save(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex){
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique")) {
                message = String.format("Ошибка. Пользователь с email %s уже существует", user.getEmail());
                debugMessage = String.format("Ошибка при добавлении пользователя. " +
                        "Пользователь с email %s уже существует в институте", user.getEmail());
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }
    }

    public List<User> saveAllCSV(MultipartFile file, Institution institution, UUID groupId) {
        List<CsvParser.UserCsv> userCsvs;
        User principal = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        try {
            userCsvs = csvParser.parseCsv(file);
        } catch (IOException ex) {

            throw new IOEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при загрузке списка пользователей в csv. Ошибка при чтении файла %s" +
                                    " пользователя %s", file.getOriginalFilename(),
                            ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId())
            );

        } catch (IllegalArgumentException ex) {
            throw new InvalidFileDataFormatEx(
                    "Неправильный формат данных",
                    String.format("Ошибка при добавлении списка студентов csv. Пользователь %s пытается загрузить файл с " +
                                    "неправильным форматом данных",
                            principal.getId()
                    )
            );
        }

        List<User> users = userCsvs.stream()
                .map(csv -> {
                    Group group = null;
                    if(csv.getRole() == Role.STUDENT) {
                        if (groupId == null) {
                            if (csv.getGroupName() != null) {
                                group = groupService.findByInstitutionIdAndName(institution.getId(), csv.getGroupName())
                                        .orElseThrow(() ->
                                                new EntityNotFoundEx(
                                                        String.format("Ошибка. Группы %s в этом учебном заведении не существует",
                                                                csv.getGroupName()),
                                                        String.format("Ошибка при загрузке сsv списка пользователей " +
                                                                        "пользователем %s. Группы %s в учебном заведении %s не существует",
                                                                principal.getId(), csv.getGroupName(), institution.getId())
                                                )
                                        );
                            } else{
                                throw new InvalidFileDataFormatEx(
                                        String.format("Неправильный формат данных. Студент %s %s не имеет группы",
                                                csv.getSurname(), csv.getName()),
                                        String.format("Ошибка при добавлении списка студентов csv. Пользователь %s пытается загрузить файл с " +
                                                        "неправильным форматом данных",
                                                principal.getId()
                                        )
                                );
                            }
                        } else {
                            group = groupService.findById(groupId)
                                    .orElseThrow(() ->
                                            new EntityNotFoundEx(
                                                    String.format("Группы %s в этом учебном заведении не существует",
                                                            csv.getGroupName()),
                                                    String.format("Ошибка при загрузке сsv списка пользователей" +
                                                                    " пользователем %s. Группы %s в учебном заведении %s не существует",
                                                            principal.getId(), csv.getGroupName(), institution.getId())
                                            )
                                    );
                        }
                    }

                    // todo ИСПРАВИТЬ isEnabled(false)
                    return User
                            .builder()
                            .name(csv.getName())
                            .surname(csv.getSurname())
                            .patronymic(csv.getPatronymic())
                            .institution(institution)
                            .group(group)
                            .role(csv.getRole())
                            .age(csv.getAge())
                            .email(csv.getEmail())
                            .isEnabled(true)
                            .tfaSecret(null)
                            .isTfaEnabled(false)
                            .password("{noop}" + passwordGenerator.generateRawPassword(PASSWORD_LENGTH, PASSWORD_GEN_RULES))
                            .build();
                })
                .toList();

        try {
            return userRepository.saveAllAndFlush(users);
        } catch (DataIntegrityViolationException ex) {
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique")) {
                message = "Ошибка. Один или несколько указанных пользователей уже существуют";
                debugMessage = String.format("Ошибка при добавлении csv списка пользователей пользователем %s. " +
                        "Один или несколько указанных пользователей уже существуют", principal.getId());
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = String.format("Ошибка при добавлении csv списка пользователей пользователем %s\n%s",
                        principal.getId(), root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }
    }

    public List<User> saveAll(List<User> users) {
        try {
            return userRepository.saveAllAndFlush(users);
        } catch (DataIntegrityViolationException ex) {
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique")) {
                message = "Ошибка. Один или несколько указанных пользователей уже существуют";
                debugMessage = "Ошибка при добавлении списка пользователей. " +
                        "Один или несколько указанных пользователей уже существуют";
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }
    }

    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
    }

    public List<User> findAllByInstitutionIdAndGroup(UUID institutionId, String group) {
        return userRepository.findAllByInstitutionIdAndGroup(institutionId, group);
    }

    public List<User> findAllByInstitutionId(UUID institutionId) {
        return userRepository.findAllByInstitutionId(institutionId);
    }

    public void deleteById(UUID userId) {
        userRepository.deleteById(userId);
    }

    public void deleteByEmail(String email) {
        userRepository.deleteByEmail(email);
    }

    public List<User> findAllByCourseId(UUID courseId) {
        return userRepository.findAllByCourseId(courseId);
    }

    public List<User> findAllByInstitutionIdNotInCourse(UUID institutionId, UUID courseId){
        return userRepository.findAllByInstitutionIdNotInCourse(institutionId, courseId);
    }
}
