package org.monostudio.jpa.exceptions;

import lombok.NoArgsConstructor;

import jakarta.persistence.EntityNotFoundException;

@NoArgsConstructor
public class UserNotFoundException
    extends EntityNotFoundException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
