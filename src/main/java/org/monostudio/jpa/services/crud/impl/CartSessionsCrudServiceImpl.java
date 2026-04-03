package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.repositories.CartSessionsRepository;
import org.monostudio.jpa.services.conversion.CartSessionsConverterService;
import org.monostudio.jpa.services.crud.CartSessionsCrudService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.patch.CartSessionsPatchService;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Transactional
@Service
public class CartSessionsCrudServiceImpl
    extends CrudGenericService<CartSessionPojo, CartSession>
    implements CartSessionsCrudService {

    private final CartSessionsRepository cartSessionsRepository;

    @Autowired
    public CartSessionsCrudServiceImpl(
        CartSessionsRepository cartSessionsRepository,
        CartSessionsConverterService cartSessionsConverterService,
        CartSessionsPatchService cartSessionsPatchService
    ) {
        super(cartSessionsRepository, cartSessionsConverterService, cartSessionsPatchService);
        this.cartSessionsRepository = cartSessionsRepository;
    }

    @Override
    public Optional<CartSession> getExisting(CartSessionPojo input) throws BadInputException {
        String token = input.getToken();
        if (token == null || token.isBlank()) {
            throw new BadInputException("Invalid cart session token");
        }
        return cartSessionsRepository.findByToken(token);
    }
}
