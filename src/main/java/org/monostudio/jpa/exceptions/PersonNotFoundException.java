package org.monostudio.jpa.exceptions;

import lombok.NoArgsConstructor;

import jakarta.persistence.EntityNotFoundException;

@NoArgsConstructor
public class PersonNotFoundException
    extends EntityNotFoundException {

    public PersonNotFoundException(String message) {
        super(message);
    }
}
