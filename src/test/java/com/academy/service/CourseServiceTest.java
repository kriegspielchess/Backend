package com.academy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.entity.Course;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void findAllReturnsCoursesFromRepository() {
        List<Course> courses = List.of(new Course());
        when(courseRepository.findAll()).thenReturn(courses);

        assertSame(courses, courseService.findAll());
    }

    @Test
    void findByIdReturnsCourseOrReportsMissingCourse() {
        Course course = new Course();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        assertSame(course, courseService.findById(1L));
        assertEquals("Course not found with id: 2",
                assertThrows(ResourceNotFoundException.class, () -> courseService.findById(2L)).getMessage());
    }

    @Test
    void createClearsClientSuppliedIdBeforeSaving() {
        Course course = Course.builder().id(99L).title("Beginner").build();
        when(courseRepository.save(course)).thenReturn(course);

        assertSame(course, courseService.create(course));

        assertNull(course.getId());
        verify(courseRepository).save(course);
    }

    @Test
    void updateCopiesAllMutableFieldsAndRejectsMissingCourse() {
        Course existing = Course.builder().id(1L).title("Old").description("Old description")
                .instructor("Old instructor").durationHours(1).price(1.0).build();
        Course updated = Course.builder().id(2L).title("New").description("New description")
                .instructor("New instructor").durationHours(2).price(2.0).build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(existing)).thenReturn(existing);
        when(courseRepository.findById(3L)).thenReturn(Optional.empty());

        assertSame(existing, courseService.update(1L, updated));

        assertEquals("New", existing.getTitle());
        assertEquals("New description", existing.getDescription());
        assertEquals("New instructor", existing.getInstructor());
        assertEquals(2, existing.getDurationHours());
        assertEquals(2.0, existing.getPrice());
        assertThrows(ResourceNotFoundException.class, () -> courseService.update(3L, updated));
    }

    @Test
    void deleteOnlyRemovesExistingCourse() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.existsById(2L)).thenReturn(false);

        courseService.delete(1L);

        verify(courseRepository).deleteById(1L);
        assertThrows(ResourceNotFoundException.class, () -> courseService.delete(2L));
    }
}
