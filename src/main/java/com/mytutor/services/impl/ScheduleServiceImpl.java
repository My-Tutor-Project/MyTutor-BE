package com.mytutor.services.impl;

import com.mytutor.constants.AppointmentStatus;
import com.mytutor.dto.timeslot.InputWeeklyScheduleDto;
import com.mytutor.dto.timeslot.ScheduleDto;
import com.mytutor.dto.timeslot.ScheduleItemDto;
import com.mytutor.dto.timeslot.TimeslotDto;
import com.mytutor.entities.Account;
import com.mytutor.entities.Timeslot;
import com.mytutor.entities.WeeklySchedule;
import com.mytutor.exceptions.AccountNotFoundException;
import com.mytutor.exceptions.TimeslotValidationException;
import com.mytutor.repositories.AccountRepository;
import com.mytutor.repositories.TimeslotRepository;
import com.mytutor.repositories.WeeklyScheduleRepository;
import com.mytutor.services.ScheduleService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author vothimaihoa
 *
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    TimeslotRepository timeslotRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    ModelMapper modelMapper;
    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Override
    public ResponseEntity<?> addNewSchedule(Integer tutorId, List<InputWeeklyScheduleDto> weeklyScheduleDtos) {
        try {

            Account account = accountRepository.findById(tutorId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found!"));

            List<WeeklySchedule> overlapSchedules = saveValidTimeslotAndGetOverlapTimeslot(weeklyScheduleDtos, account);

            if (overlapSchedules.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                    .body("All timeslots are saved successfully");
            } else {
                // return overlapped timeslot to FE to show to the customer
                // FE will show annoucement that timeslots saved, except these slots are overlap...
                return ResponseEntity.status(HttpStatus.OK)
                        .body(overlapSchedules);
            }

        } catch (AccountNotFoundException | TimeslotValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while saving the schedule.");
        }
    }

    private List<WeeklySchedule> saveValidTimeslotAndGetOverlapTimeslot
            (List<InputWeeklyScheduleDto> inputWeeklyScheduleDtos,
            Account account)
            throws TimeslotValidationException {
        List<WeeklySchedule> overlapSchedules = new ArrayList<>();
        List<WeeklySchedule> validatedSchedules = new ArrayList<>();
            for (InputWeeklyScheduleDto inputWeeklyScheduleDto : inputWeeklyScheduleDtos) {
                WeeklySchedule schedule = modelMapper.map(inputWeeklyScheduleDto, WeeklySchedule.class);
                if (weeklyScheduleRepository.findOverlapSchedule(
                        account.getId(),
                        inputWeeklyScheduleDto.getDayOfWeek(),
                        inputWeeklyScheduleDto.getStartTime(),
                        inputWeeklyScheduleDto.getEndTime()).isEmpty()) {
                    schedule.setAccount(account);
                    schedule.setUsing(true);
                    validatedSchedules.add(schedule);
                } else {
                    overlapSchedules.add(schedule);
                }
        }
        weeklyScheduleRepository.saveAll(validatedSchedules);
        return overlapSchedules;
    }

    // update schedule => kiem tra slot can them da có trong db chua (dayOfWeek, startTime, endTime trung)
    // co roi thi de nguyen,
    // chua co thi them vao,
    // old schedule co ma new ko co => xoa di
    @Override
    public ResponseEntity<?> updateSchedule(Integer tutorId, List<InputWeeklyScheduleDto> newSchedules) {

        Account account = accountRepository.findById(tutorId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        // Fetch existing schedules for the tutor
        List<WeeklySchedule> existingSchedules = weeklyScheduleRepository.findByTutorId(tutorId);

        // Create maps for quick lookup
        Map<String, InputWeeklyScheduleDto> newScheduleMap = newSchedules.stream()
                .collect(Collectors.toMap(
                        s -> s.getDayOfWeek()
                                + "-" + s.getStartTime()
                                + "-" + s.getEndTime(),
                        s -> s
                ));

        Map<String, WeeklySchedule> existingScheduleMap = existingSchedules.stream()
                .collect(Collectors.toMap(
                        s -> s.getDayOfWeek()
                                + "-" + s.getStartTime()
                                + "-" + s.getEndTime(),
                        s -> s
                ));

        // Add or update schedules
        for (InputWeeklyScheduleDto newSchedule : newSchedules) {
            String key = newSchedule.getDayOfWeek() + "-"
                    + newSchedule.getStartTime() + "-"
                    + newSchedule.getEndTime();
            if (!existingScheduleMap.containsKey(key)) {
                // Schedule does not exist, add it
                WeeklySchedule newScheduleDB = modelMapper.map(newSchedule, WeeklySchedule.class);
                newScheduleDB.setAccount(account);
                newScheduleDB.setUsing(true); // Ensure new schedules are marked as using
                weeklyScheduleRepository.save(newScheduleDB);
            } else {
                // Schedule exists, ensure it's marked as using
                WeeklySchedule existingSchedule = existingScheduleMap.get(key);
                existingSchedule.setUsing(true);
                weeklyScheduleRepository.save(existingSchedule);
            }
        }

        // Mark old schedules that are not in the new schedules as not using
        for (WeeklySchedule existingSchedule : existingSchedules) {
            String key = existingSchedule.getDayOfWeek() + "-"
                    + existingSchedule.getStartTime()
                    + "-" + existingSchedule.getEndTime();
            if (!newScheduleMap.containsKey(key)) {
                if (!existingSchedule.getTimeslots().isEmpty()) {
                    existingSchedule.setUsing(false);
                    weeklyScheduleRepository.save(existingSchedule);
                }
                else {
                    weeklyScheduleRepository.delete(existingSchedule);
                }
            }
        }

        return ResponseEntity.ok().body("Schedule updated successfully");
    }

    // lay ra cac timeslot cua 7 ngay gan nhat theo schedule
    // Tính timeslots dựa theo schedule và xuất ra
    @Override
    public ResponseEntity<?> getTutorWeeklySchedule(Integer tutorId) {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(6);

        ScheduleDto scheduleDto = new ScheduleDto();
        List<ScheduleItemDto> items = scheduleDto.getSchedules();

        int today = startDate.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < 7; i++) {
            int d = (today + i) % 7 + 2; // Monday is 2 and Sunday is 8
            List<WeeklySchedule> weeklySchedules = weeklyScheduleRepository
                    .findByTutorIdAnDayOfWeek(tutorId, d);
            LocalDate date = startDate.plusDays(i);
            removeBookedSlot(weeklySchedules, date);

            if(weeklySchedules.isEmpty()) {
                weeklySchedules = new ArrayList<>();
            }
            String dayOfWeek = date.getDayOfWeek().toString().substring(0, 3);
            int dayOfMonth = date.getDayOfMonth();
            List<TimeslotDto> timeslotDtos = weeklySchedules.stream()
                    .map(t -> TimeslotDto.mapToDto(t)).toList();

            ScheduleItemDto scheduleItemDto = new ScheduleItemDto(dayOfWeek, dayOfMonth, timeslotDtos);
            items.add(scheduleItemDto);
        }

        scheduleDto.setStartDate(startDate);
        scheduleDto.setEndDate(endDate);

        return ResponseEntity.status(HttpStatus.OK).body(scheduleDto);
    }

    private void removeBookedSlot(List<WeeklySchedule> weeklySchedules, LocalDate date) {
        weeklySchedules.removeIf(weeklySchedule ->
                timeslotRepository.findTimeslotWithDateAndWeeklySchedule(weeklySchedule.getId(), date) != null);
    }



}
