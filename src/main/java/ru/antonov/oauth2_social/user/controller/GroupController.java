package ru.antonov.oauth2_social.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.user.dto.*;
import ru.antonov.oauth2_social.user.entity.EntityFactory;
import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.service.GroupService;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.exception.AccessDeniedEx;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Validated
public class GroupController {
    private final GroupService groupService;
    private final AccessManager accessManager;

    @PostMapping("/batch")
    public ResponseEntity<List<GroupResponseDto>> addGroupsBatch(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody List<GroupCreateRequestDto> requestList
    ){
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        List<Group> groupEntities = requestList.stream()
                .map(dto ->
                        EntityFactory.makeGroupEntity(dto, principal.getInstitution())
                )
                .toList();

        List<Group> addedGroups = groupService.saveAll(groupEntities);

        return ResponseEntity.ok(
                addedGroups.stream()
                        .map(DtoFactory::makeGroupResponseDto)
                        .toList()
        );
    }

    @PostMapping
    public ResponseEntity<GroupResponseDto> addGroup(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody GroupCreateRequestDto request
    ){
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        Group group = EntityFactory.makeGroupEntity(request, principal.getInstitution());
        group = groupService.save(group);

        return ResponseEntity.ok(DtoFactory.makeGroupResponseDto(group));
    }

    @GetMapping("/institution")
    public ResponseEntity<List<GroupResponseDto>> findGroupsByInstitutionId(
            @AuthenticationPrincipal User principal,
            @RequestParam("institution_id") UUID institutionId
    ){
        checkUserHasAccessToInstitutionOrElseThrow(principal, institutionId);

        List<Group> groups = groupService.findAllByInstitutionId(institutionId);

        return ResponseEntity.ok(
                groups.stream()
                        .map(DtoFactory::makeGroupResponseDto)
                        .toList()
        );
    }

    @DeleteMapping("/id")
    public ResponseEntity<?> deleteGroupById(
            @AuthenticationPrincipal User principal,
            @RequestParam("group_id") UUID groupId
    ){
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Группы с указанным id не существует",
                        String.format("Неудача при поиске группы. Группа с %s id не существует", groupId)
                ));

        checkUserHasAccessToInstitutionOrElseThrow(principal, group.getInstitution().getId());

        groupService.deleteById(groupId);

        return ResponseEntity.ok().build();
    }

    private void checkPrincipalIsAttachedToInstitutionOrElseThrow(User principal){
        if(principal.getInstitution() == null){
            throw new AccessDeniedEx(
                    "Ошибка. Вы не привязаны к учебному заведению",
                    String.format("Отказ в доступе при добавлении списка пользователей. Пользователь %s не привязан к учебному заведению",
                            principal.getEmail())
            );
        }
    }

    private void checkUserHasAccessToInstitutionOrElseThrow(User user, UUID institutionId){
        if(!accessManager.isUserHasAccessToInstitution(user, institutionId)){
            throw new AccessDeniedEx(
                    "Ошибка. У вас нет доступа к этому учебному заведению",
                    String.format("Отказ в доступе. Пользователю %s не разрешен доступ к институту %s", user.getEmail(), institutionId)
            );
        }
    }
}
