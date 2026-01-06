package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.model.CalendarEvent;
import org.moinex.model.enums.CalendarEventType;
import org.moinex.repository.CalendarEventRepository;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock private CalendarEventRepository calendarEventRepository;

    @InjectMocks private CalendarService calendarService;

    private CalendarEvent event;

    @BeforeEach
    void setUp() {
        event =
                CalendarEvent.builder()
                        .id(1)
                        .title("Test Event")
                        .description("Test Description")
                        .date(LocalDateTime.now().plusDays(1))
                        .eventType(CalendarEventType.DEBT_PAYMENT_DUE_DATE)
                        .build();
    }

    @Test
    @DisplayName("Should add event successfully when data is valid")
    void addEvent_Success() {
        ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);
        String title = "  My Event  ";
        String description = "Event Description";
        LocalDateTime date = LocalDateTime.now();
        CalendarEventType type = CalendarEventType.DEBT_PAYMENT_DUE_DATE;

        calendarService.addEvent(title, description, date, type);

        verify(calendarEventRepository).save(eventCaptor.capture());
        CalendarEvent capturedEvent = eventCaptor.getValue();

        assertEquals(title.strip(), capturedEvent.getTitle());
        assertEquals(description, capturedEvent.getDescription());
        assertEquals(
                date.truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
                capturedEvent.getDate().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        assertEquals(type, capturedEvent.getEventType());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding an event with an empty title")
    void addEvent_EmptyTitle_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calendarService.addEvent(
                                "   ",
                                "Description",
                                LocalDateTime.now(),
                                CalendarEventType.DEBT_PAYMENT_DUE_DATE));

        verify(calendarEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete event successfully when event exists")
    void deleteEvent_Success() {
        when(calendarEventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        calendarService.deleteEvent(event.getId());

        verify(calendarEventRepository).delete(event);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a non-existent event")
    void deleteEvent_NotFound_ThrowsException() {
        when(calendarEventRepository.findById(event.getId())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class, () -> calendarService.deleteEvent(event.getId()));

        verify(calendarEventRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update event successfully")
    void updateEvent_Success() {
        ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);
        event.setTitle("Updated Title");

        calendarService.updateEvent(event);

        verify(calendarEventRepository).save(eventCaptor.capture());
        assertEquals("Updated Title", eventCaptor.getValue().getTitle());
    }

    @Test
    @DisplayName("Should get all events successfully")
    void getAllEvents_Success() {
        when(calendarEventRepository.findAll()).thenReturn(Collections.singletonList(event));

        List<CalendarEvent> events = calendarService.getAllEvents();

        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals(event, events.get(0));
        verify(calendarEventRepository).findAll();
    }
}
