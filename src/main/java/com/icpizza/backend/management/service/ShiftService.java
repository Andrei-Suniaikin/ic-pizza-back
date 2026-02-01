package com.icpizza.backend.management.service;

import com.icpizza.backend.management.dto.shift.BaseShiftResponse;
import com.icpizza.backend.management.dto.shift.CreateShiftReportTO;
import com.icpizza.backend.management.dto.shift.EditShiftReportTO;
import com.icpizza.backend.management.dto.shift.ShiftReportTO;
import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.entity.Shift;
import com.icpizza.backend.management.enums.ReportType;
import com.icpizza.backend.management.mapper.ShiftMapper;
import com.icpizza.backend.management.repository.ReportRepository;
import com.icpizza.backend.management.repository.ShiftRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {
    private final ShiftRepository shiftRepository;
    private final ReportRepository reportRepository;
    private final ShiftMapper shiftMapper;

    public List<BaseShiftResponse> getAllShiftReports() {
        log.info("[MANAGEMENT SHIFT] Getting all shift reports");
        List<Report> reports = reportRepository.findAllByType(ReportType.SHIFT_REPORT);
        return shiftMapper.toBaseShiftResponse(reports);
    }

    @Transactional
    public BaseShiftResponse createShiftReport(CreateShiftReportTO createShiftReportTO) {
        log.info("[MANAGEMENT SHIFT] Creating shift report {}", createShiftReportTO);
        try {
            Report report = shiftMapper.toReportEntity(createShiftReportTO);
            log.info("[MANAGEMENT SHIFT] Converted report {}", report);
            reportRepository.save(report);
            List<Shift> shifts = shiftMapper.toShiftEntities(createShiftReportTO.shifts(), report);
            log.info("[MANAGEMENT SHIFT] Converted shifts {}", shifts);
            shiftRepository.saveAll(shifts);
            return shiftMapper.toBaseShiftResponse(report);
        }
        catch (Exception e) {
            log.error("[MANAGEMENT SHIFT] Failed to create shift report", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create shift report");
        }
    }

    @Transactional
    public BaseShiftResponse editShiftReport(EditShiftReportTO editShiftReportTO) {
        log.info("[MANAGEMENT SHIFT] Editing shift report with id {}.", editShiftReportTO.id());
        try{
            Report report = reportRepository.findById(editShiftReportTO.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
            List<Shift> shifts = shiftRepository.findAllByReport(report);
            shiftRepository.deleteAll(shifts);
            List<Shift> updatedShifts = shiftMapper.toShiftEntitiesAfterEdit(editShiftReportTO.shifts(), report);
            shiftRepository.saveAll(updatedShifts);
            return shiftMapper.toBaseShiftResponse(report);
        }
        catch (Exception e) {
            log.error("[MANAGEMENT SHIFT] Failed to edit shift report", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ShiftReportTO findReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        List<Shift> shifts = shiftRepository.findAllByReport(report);
        return shiftMapper.toShiftTO(report, shifts);
    }
}
