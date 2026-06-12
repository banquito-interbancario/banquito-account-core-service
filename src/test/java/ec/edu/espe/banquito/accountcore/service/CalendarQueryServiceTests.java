package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.dto.HolidayCheckResponseDTO;
import ec.edu.espe.banquito.accountcore.model.Holiday;
import ec.edu.espe.banquito.accountcore.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarQueryServiceTests {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private CalendarQueryService calendarQueryService;

    @Test
    void shouldReturnRegisteredHoliday() {
        LocalDate date = LocalDate.of(2026, Month.DECEMBER, 25);
        Holiday holiday = new Holiday();
        holiday.setHolidayDate(date);
        holiday.setName("Navidad");
        holiday.setIsWeekend(false);
        when(holidayRepository.findById(date)).thenReturn(Optional.of(holiday));

        HolidayCheckResponseDTO response = calendarQueryService.checkHoliday(date);

        assertTrue(response.holiday());
        assertEquals("Navidad", response.name());
        assertFalse(response.weekend());
    }

    @Test
    void shouldReturnFalseWhenDateIsNotRegistered() {
        LocalDate date = LocalDate.of(2026, Month.DECEMBER, 24);
        when(holidayRepository.findById(date)).thenReturn(Optional.empty());

        HolidayCheckResponseDTO response = calendarQueryService.checkHoliday(date);

        assertFalse(response.holiday());
        assertFalse(response.weekend());
        assertNull(response.name());
    }
}
