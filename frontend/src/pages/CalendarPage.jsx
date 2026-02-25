import { useState, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { calendar } from '../api/endpoints';
import EventCreateModal from '../components/EventCreateModal';

export default function CalendarPage() {
  const [events, setEvents] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchEvents = useCallback(async (fetchInfo) => {
    setLoading(true);
    try {
      const start = fetchInfo.startStr;
      const end = fetchInfo.endStr;
      const res = await calendar.getTeamEvents(start, end);
      const data = res.data.data || [];

      setEvents(
        data.map((e) => ({
          id: e.id,
          title: e.title,
          start: e.startTime,
          end: e.endTime,
          allDay: e.isAllDay || e.allDay,
          extendedProps: {
            description: e.description,
            location: e.location,
            attendees: e.attendees,
          },
        }))
      );
    } catch (err) {
      console.error('일정 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleDateClick = (info) => {
    setSelectedDate(info.dateStr);
    setModalOpen(true);
  };

  const handleCreateEvent = async (payload) => {
    try {
      await calendar.createEvent(payload);
      setEvents((prev) => [
        ...prev,
        {
          id: 'temp-' + Date.now(),
          title: payload.title,
          start: payload.startTime,
          end: payload.endTime,
          allDay: payload.isAllDay,
        },
      ]);
    } catch (err) {
      console.error('일정 생성 실패:', err);
      throw err;
    }
  };

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900 dark:text-white">캘린더</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            팀 일정을 확인하고 새 일정을 생성합니다.
          </p>
        </div>
        <button
          onClick={() => {
            setSelectedDate(new Date().toISOString().split('T')[0]);
            setModalOpen(true);
          }}
          className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white text-sm font-medium rounded-lg transition-colors flex items-center gap-2"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          새 일정
        </button>
      </div>

      <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-4">
        {loading && (
          <div className="absolute top-4 right-4 z-10">
            <div className="w-5 h-5 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="dayGridMonth"
          headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek',
          }}
          locale="ko"
          events={events}
          datesSet={fetchEvents}
          dateClick={handleDateClick}
          height="auto"
          buttonText={{
            today: '오늘',
            month: '월간',
            week: '주간',
          }}
          dayMaxEventRows={3}
          eventDisplay="block"
          eventClassNames="!bg-primary-500 !border-primary-500 !text-white !text-xs !rounded-md !px-1"
        />
      </div>

      <EventCreateModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSubmit={handleCreateEvent}
        initialDate={selectedDate}
      />
    </div>
  );
}
