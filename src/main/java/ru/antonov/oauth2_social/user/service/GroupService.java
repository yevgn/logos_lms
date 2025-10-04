package ru.antonov.oauth2_social.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.repository.GroupRepository;
import ru.antonov.oauth2_social.exception.DBConstraintViolationEx;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {
    private final GroupRepository groupRepository;

    public List<Group> saveAll(List<Group> groups){
        try {
            return groupRepository.saveAllAndFlush(groups);
        } catch (DataIntegrityViolationException ex){
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique")) {
                message = "Одна или несколько выбранных вами групп уже существуют в этом институте";
                debugMessage = String.format(
                        "Ошибка при добавлении групп. Конфликт уникальности по данным: группы %s, институт %s",
                        groups, groups.get(0).getInstitution().getId()
                );
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }
    }

    public Group save(Group group){
        try{
            return groupRepository.save(group);
        } catch (DataIntegrityViolationException ex){
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique")) {
                message = String.format("Группа %s уже существует в этом институте", group.getName());
                debugMessage = String.format("Ошибка при добавлении группы. " +
                        "Группа %s уже существует в институте %s", group.getName(), group.getInstitution().getId());
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }
    }


    public List<Group> findAllByInstitutionId(UUID institutionId){
        return groupRepository.findAllByInstitutionId(institutionId);
    }

    public Optional<Group> findByInstitutionIdAndName(UUID institutionId, String name){
        return groupRepository.findByInstitutionIdAndName(institutionId, name);
    }

    public void deleteByInstitutionIdAndName(UUID institutionId, String name) {
        groupRepository.deleteByInstitutionIdAndName(institutionId, name);
    }

    public void deleteById(UUID groupId) {
        groupRepository.deleteById(groupId);
    }

    public Optional<Group> findById(UUID groupId) {
        return groupRepository.findById(groupId);
    }
}
