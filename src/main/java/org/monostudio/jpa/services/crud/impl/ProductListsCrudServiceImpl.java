package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductListPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductList;
import org.monostudio.jpa.repositories.ProductListItemsRepository;
import org.monostudio.jpa.repositories.ProductListsRepository;
import org.monostudio.jpa.services.conversion.ProductListsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ProductListCrudService;
import org.monostudio.jpa.services.patch.ProductListsPatchService;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Transactional
@Service
public class ProductListsCrudServiceImpl
    extends CrudGenericService<ProductListPojo, ProductList>
    implements ProductListCrudService {
    private final ProductListsRepository listsRepository;
    private final ProductListItemsRepository listItemsRepository;

    @Autowired
    public ProductListsCrudServiceImpl(
        ProductListsRepository listsRepository,
        ProductListItemsRepository listItemsRepository,
        ProductListsConverterService listsConverterService,
        ProductListsPatchService listsPatchService
    ) {
        super(listsRepository, listsConverterService, listsPatchService);
        this.listsRepository = listsRepository;
        this.listItemsRepository = listItemsRepository;
    }

    @Override
    public void delete(Predicate filters)
        throws EntityNotFoundException {
        long count = listsRepository.count(filters);
        if (count==0) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        } else {
            for (ProductList list : listsRepository.findAll(filters)) {
                listItemsRepository.deleteByListId(list.getId());
            }

            listsRepository.deleteAll(listsRepository.findAll(filters));
        }
    }

    @Override
    public Optional<ProductList> getExisting(ProductListPojo input) throws BadInputException {
        String name = input.getName();
        if (StringUtils.isBlank(name)) {
            throw new BadInputException("The specified list has no name");
        } else {
            return listsRepository.findByName(name);
        }
    }
}
