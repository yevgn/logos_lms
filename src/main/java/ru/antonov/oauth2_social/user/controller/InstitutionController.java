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
import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.user.dto.DtoFactory;
import ru.antonov.oauth2_social.user.dto.InstitutionCreateRequestDto;
import ru.antonov.oauth2_social.user.dto.InstitutionResponseDto;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.exception.InstitutionAlreadyCreatedEx;
import ru.antonov.oauth2_social.user.service.InstitutionService;
import ru.antonov.oauth2_social.user.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/institutions")
@RequiredArgsConstructor
@Validated
public class InstitutionController {
    private final InstitutionService institutionService;
    private final UserService userService;
    private final AccessManager accessManager;

    @Operation(
            summary = "Добавление учебного заведения",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление учебными заведениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Нет доступа", content = @Content()),
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
                    ))
    }
    )
    @PostMapping
    public ResponseEntity<InstitutionResponseDto> saveInstitution(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody InstitutionCreateRequestDto request
    ){
        checkPrincipalIsNotAttachedToInstitutionOrElseThrow(principal);

        return ResponseEntity.ok(institutionService.saveInstitution(principal, request));
    }


    @Operation(
            summary = "Получение информации об учебном заведении по id",
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
    @GetMapping
    public ResponseEntity<InstitutionResponseDto> findInstitutionById(
            @AuthenticationPrincipal User principal,
            @RequestParam("institution_id") UUID institutionId
    ){
        checkPrincipalHasAccessToInstitutionOrElseThrow(principal, institutionId);

        return ResponseEntity.ok(institutionService.findInstitutionById(institutionId));
    }

    @Operation(
            summary = "Получение информации о пользователях по id учебного заведения",
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
                                          "message": "параметр institution_id отсутствует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/users")
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

    @Operation(
            summary = "Получение информации о пользователях по id учебного заведения и группе",
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
                                          "message": "параметр institution_id отсутствует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/users/group")
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

    private void checkPrincipalIsNotAttachedToInstitutionOrElseThrow(User principal){
        if(principal.getInstitution() != null){
            throw new InstitutionAlreadyCreatedEx(
                    "Вы уже привязаны к учебному заведению",
                    String.format(
                            "Ошибка добавления учебного заведения. Пользователь %s ранее уже добавил учебное заведение",
                            principal.getId())
            );
        }
    }

    private void checkPrincipalHasAccessToInstitutionOrElseThrow(User principal, UUID institutionId) {
        if (!accessManager.isUserHasAccessToInstitution(principal, institutionId)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому учебному заведению.",
                    String.format("Пользователю %s не разрешен доступ к учебному заведению %s", principal.getEmail(), institutionId)
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
}
