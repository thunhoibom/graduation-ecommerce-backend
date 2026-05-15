package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.repositories.CartItemsRepository;
import org.monostudio.jpa.services.conversion.CartItemsConverterService;
import org.monostudio.jpa.services.crud.CartItemsCrudService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.patch.CartItemsPatchService;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Transactional
@Service
public class CartItemsCrudServiceImpl
    extends CrudGenericService<CartItemPojo, CartItem>
    implements CartItemsCrudService {

    @Autowired
    public CartItemsCrudServiceImpl(
        CartItemsRepository cartItemsRepository,
        CartItemsConverterService cartItemsConverterService,
        CartItemsPatchService cartItemsPatchService
    ) {
        super(cartItemsRepository, cartItemsConverterService, cartItemsPatchService);
    }

    /**
     * CartItems are managed exclusively through CartService.
     * Direct CRUD via admin API is not supported.
     */
    @Override
    public Optional<CartItem> getExisting(CartItemPojo input) throws BadInputException {
        throw new UnsupportedOperationException(
            "CartItems are managed via CartService, not direct CRUD");
    }
}
