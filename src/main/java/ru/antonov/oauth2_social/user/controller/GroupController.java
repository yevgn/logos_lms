package ru.antonov.oauth2_social.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ru.antonov.oauth2_social.common.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.common.exception.AccessDeniedEx;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Validated
public class GroupController {
    private final GroupService groupService;
    private final AccessManager accessManager;

    @Operation(
            summary = "Добавление учебных групп в учебное заведение",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление группами")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому учебному заведению"
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
                                          "message": "поле name не должно быть пустым"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Дублирующиеся данные",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Одна или несколько выбранных вами групп уже существуют в этом институте"
                                        }
                                    """)
                    )),

    }
    )
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

    @Operation(
            summary = "Добавление учебной группы в учебное заведение",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление группами")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому учебному заведению"
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
                                          "message": "поле name не должно быть пустым"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Дублирующиеся данные",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Эта группа уже существует в этом институте"
                                        }
                                    """)
                    )),

    }
    )
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

    @Operation(
            summary = "Получении информации об учебных группах в учебном заведении с указанным id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление группами")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому учебному заведению"
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
                                          "message": "Параметр institution_id отсутствует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<GroupResponseDto>> findGroupsByInstitutionId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID institutionId
    ){
        checkUserHasAccessToInstitutionOrElseThrow(principal, institutionId);

        List<Group> groups = groupService.findAllByInstitutionId(institutionId);

        return ResponseEntity.ok(
                groups.stream()
                        .map(DtoFactory::makeGroupResponseDto)
                        .toList()
        );
    }

    @Operation(
            summary = "Получении информации об учебной группе с указанным id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление группами")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этой группе"
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
                                          "message": "Параметр group_id отсутствует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDto> findGroupById(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID groupId
    ){
        checkUserHasAccessToGroupOrElseThrow(principal, groupId);

        return ResponseEntity.ok(groupService.findGroupById(groupId));
    }

    @Operation(
            summary = "Удаление группы по id",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление группами")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому учебному заведению"
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
                                          "message": "Группы с указанным id не существуетv"
                                        }
                                    """)
                    ))
    }
    )
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroupById(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID groupId
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
                    String.format("Отказ в доступе при добавлении списка групп. Пользователь %s не привязан к учебному заведению",
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

    private void checkUserHasAccessToGroupOrElseThrow(User user, UUID groupId){
        if(!accessManager.isUserHasAccessToGroup(user, groupId)){
            throw new AccessDeniedEx(
                    "Ошибка. У вас нет доступа к этой группе",
                    String.format("Отказ в доступе. Пользователю %s не разрешен доступ к группе %s", user.getEmail(), groupId)
            );
        }
    }
}
