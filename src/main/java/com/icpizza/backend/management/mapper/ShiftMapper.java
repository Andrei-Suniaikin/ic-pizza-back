package com.icpizza.backend.management.mapper;

import com.icpizza.backend.management.dto.shift.BaseShiftResponse;
import com.icpizza.backend.management.dto.shift.CreateShiftReportTO;
import com.icpizza.backend.management.dto.shift.EditShiftReportTO;
import com.icpizza.backend.management.dto.shift.ShiftReportTO;
import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.entity.Shift;
import com.icpizza.backend.management.enums.ReportType;
import com.icpizza.backend.management.repository.ShiftRepository;
import com.icpizza.backend.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShiftMapper {
    private final ShiftRepository shiftRepository;
    private final BranchRepository branchRepository;
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");


    public List<BaseShiftResponse> toBaseShiftResponse(List<Report> reports) {
        return reports.stream()
                .map(report -> {return new BaseShiftResponse(
                        report.getId(),
                        report.getTitle(),
                        report.getBranch().getBranchNumber(),
                        shiftRepository.sumTotalByReport(report));
                }).toList();
    }

    public Report toReportEntity(CreateShiftReportTO createShiftReportTO) {
        Report newReport = new Report();
        newReport.setType(ReportType.SHIFT_REPORT);
        newReport.setBranch(branchRepository.findByBranchNumber(createShiftReportTO.branchNo()));
        newReport.setTitle(createShiftReportTO.title());
        newReport.setCreatedAt(LocalDateTime.now(BAHRAIN));
        newReport.setFinalPrice(BigDecimal.ZERO);
        return newReport;
    }

    public List<Shift> toShiftEntities(List<CreateShiftReportTO.ShiftInfoTO> shifts, Report report) {
        List<Shift> shiftEntities = new ArrayList<>();
        shifts.forEach(shift -> {
            Shift newShift = new Shift();
            newShift.setReport(report);
            newShift.setEndShift(shift.endTime());
            newShift.setStartShift(shift.startTime());
            newShift.setShiftDate(shift.shiftDate());
            newShift.setTotalHours((shift.total() == null) ? 0.0 : shift.total());
            shiftEntities.add(newShift);
        });

        return shiftEntities;
    }

    public BaseShiftResponse toBaseShiftResponse(Report report) {
        return new BaseShiftResponse(
                report.getId(),
                report.getTitle(),
                report.getBranch().getBranchNumber(),
                shiftRepository.sumTotalByReport(report)
        );
    }

    public List<Shift> toShiftEntitiesAfterEdit(List<EditShiftReportTO.EditShiftInfoTO> shifts, Report report) {
        List<Shift> shiftEntities = new ArrayList<>();
        shifts.forEach(shift -> {
            Shift newShift = new Shift();
            newShift.setReport(report);
            newShift.setEndShift(shift.endTime());
            newShift.setStartShift(shift.startTime());
            newShift.setShiftDate(shift.shiftDate());
            newShift.setTotalHours(shift.total());
            shiftEntities.add(newShift);
        });

        return shiftEntities;
    }

    public ShiftReportTO toShiftTO(Report report, List<Shift> shifts) {
        return new ShiftReportTO(
                report.getId(),
                report.getTitle(),
                shiftRepository.sumTotalByReport(report),
                report.getCreatedAt().toLocalDate(),
                report.getBranch().getBranchNumber(),
                toShiftInfoTO(shifts)
        );
    }

    private List<ShiftReportTO.ShiftInfoTO> toShiftInfoTO(List<Shift> shifts) {
        return shifts.stream()
                .map(shift -> {return new ShiftReportTO.ShiftInfoTO(
                        shift.getShiftDate(),
                        shift.getStartShift(),
                        shift.getEndShift(),
                        shift.getTotalHours());
                }).toList();
    }
}
