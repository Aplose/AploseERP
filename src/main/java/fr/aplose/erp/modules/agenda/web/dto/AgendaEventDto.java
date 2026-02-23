package fr.aplose.erp.modules.agenda.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgendaEventDto {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotBlank
    @Size(max = 30)
    private String type = "MEETING";

    @NotBlank
    @Size(max = 20)
    private String status = "PLANNED";

    private boolean allDay = false;

    @NotNull
    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    @Size(max = 500)
    private String location;

    private Long thirdPartyId;
    private Long contactId;
    private Long projectId;

    @Size(max = 7)
    private String color;

    private boolean privacy = false;
}
