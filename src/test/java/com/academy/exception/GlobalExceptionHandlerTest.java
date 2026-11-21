package com.academy.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.academy.controller.CourseController;
import com.academy.entity.Course;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationResponseIncludesFieldErrors() throws Exception {
        Course course = new Course();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(course, "course");
        bindingResult.addError(new FieldError("course", "title", "must not be blank"));
        Method createMethod = CourseController.class.getMethod("create", Course.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(createMethod, 0), bindingResult);

        var response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> errors = assertInstanceOf(Map.class, Objects.requireNonNull(response.getBody()).get("errors"));
        assertEquals("must not be blank", errors.get("title"));
    }
}
