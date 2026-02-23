package fr.aplose.erp.modules.leave.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LeaveRequestFormDto {

    @NotNull(message = "{leave.type.required}")
    private Long leaveTypeId;

    @NotNull(message = "{leave.dateStart.required}")
    private LocalDate dateStart;

    @NotNull(message = "{leave.dateEnd.required}")
    private LocalDate dateEnd;

    private boolean halfDayStart = false;
    private boolean halfDayEnd = false;

    private String comment;
}
