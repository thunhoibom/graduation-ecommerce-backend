package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.api.services.CartService;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.CartItemsRepository;
import org.monostudio.jpa.services.conversion.CartItemsConverterService;
import org.monostudio.jpa.repositories.CartSessionsRepository;
import org.monostudio.jpa.repositories.ProductVariantsRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl
    implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartSessionsRepository cartSessionsRepository;
    private final CartItemsRepository cartItemsRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final StockReservationService stockReservationService;
    private final CartItemsConverterService cartItemsConverterService;

    @Autowired
    public CartServiceImpl(
        CartSessionsRepository cartSessionsRepository,
        CartItemsRepository cartItemsRepository,
        ProductVariantsRepository productVariantsRepository,
        StockReservationService stockReservationService,
        CartItemsConverterService cartItemsConverterService
    ) {
        this.cartSessionsRepository = cartSessionsRepository;
        this.cartItemsRepository = cartItemsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.stockReservationService = stockReservationService;
        this.cartItemsConverterService = cartItemsConverterService;
    }

    // ─── Session management ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CartSessionPojo getOrCreateSession(String sessionToken) {
        return cartSessionsRepository.findByTokenDeep(sessionToken)
            .map(this::toSessionPojo)
            .orElseGet(() -> toSessionPojo(createNewSession(sessionToken)));
    }

    @Override
    @Transactional
    public CartSessionPojo getCart(String sessionToken) {
        return cartSessionsRepository.findByTokenDeep(sessionToken)
            .map(this::toSessionPojo)
            .orElseGet(() -> toSessionPojo(createNewSession(sessionToken)));
    }

    private CartSession createNewSession(String token) {
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString().replace("-", "");
        }
        CartSession session = CartSession.builder()
            .token(token)
            .expiresAt(LocalDateTime.now().plusMinutes(CartSession.DEFAULT_TTL_MINUTES))
            .build();
        return cartSessionsRepository.saveAndFlush(session);
    }

    // ─── Item operations ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartSessionPojo addItem(String sessionToken, String variantSku, int quantity)
        throws BadInputException {
        if (quantity <= 0) {
            throw new BadInputException("Quantity must be greater than zero");
        }

        CartSession session = getOrCreateCartSession(sessionToken);
        ProductVariant variant = findActiveVariant(variantSku);

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemsRepository
            .findByCartSessionIdAndVariantId(session.getId(), variant.getId());

        if (existingItem.isPresent()) {
            // Update quantity instead of adding duplicate — add to existing
            int newQuantity = existingItem.get().getQuantity() + quantity;
            stockReservationService.updateReservation(sessionToken, variantSku, newQuantity);
            CartItem item = existingItem.get();
            item.setQuantity(newQuantity);
            cartItemsRepository.saveAndFlush(item);
            logger.info("Incremented cart item {} qty to {} (session={})",
                variantSku, newQuantity, sessionToken);
            return getCart(sessionToken);
        }

        // Reserve stock first
        stockReservationService.reserve(sessionToken, variantSku, quantity);

        // Create cart item
        CartItem item = CartItem.builder()
            .cartSession(session)
            .variant(variant)
            .quantity(quantity)
            .build();

        cartItemsRepository.saveAndFlush(item);
        logger.info("Added item {} (qty={}) to cart session {}", variantSku, quantity, sessionToken);

        clearPricingSnapshot(session);
        return getCart(sessionToken);
    }

    @Override
    @Transactional
    public CartSessionPojo updateItem(String sessionToken, String variantSku, int quantity)
        throws BadInputException {
        if (quantity <= 0) {
            return removeItem(sessionToken, variantSku);
        }

        CartSession session = getOrCreateCartSession(sessionToken);
        ProductVariant variant = findActiveVariant(variantSku);

        Optional<CartItem> existingItem = cartItemsRepository
            .findByCartSessionIdAndVariantId(session.getId(), variant.getId());

        if (existingItem.isEmpty()) {
            throw new BadInputException("Item not found in cart: " + variantSku);
        }

        int oldQuantity = existingItem.get().getQuantity();
        if (quantity == oldQuantity) {
            // No change
            return getCart(sessionToken);
        }

        // Adjust stock reservation
        stockReservationService.updateReservation(sessionToken, variantSku, quantity);

        // Update cart item
        CartItem item = existingItem.get();
        item.setQuantity(quantity);
        cartItemsRepository.saveAndFlush(item);

        logger.info("Updated cart item {} qty {} -> {} (session={})",
            variantSku, oldQuantity, quantity, sessionToken);
        clearPricingSnapshot(session);
        return getCart(sessionToken);
    }

    @Override
    @Transactional
    public CartSessionPojo removeItem(String sessionToken, String variantSku)
        throws BadInputException {
        CartSession session = cartSessionsRepository.findByToken(sessionToken).orElse(null);
        if (session == null) {
            return toSessionPojo(createNewSession(sessionToken));
        }

        ProductVariant variant = productVariantsRepository.findBySku(variantSku).orElse(null);
        if (variant != null) {
            // Release stock reservation
            stockReservationService.releaseItem(sessionToken, variantSku);

            // Delete cart item
            Optional<CartItem> item = cartItemsRepository
                .findByCartSessionIdAndVariantId(session.getId(), variant.getId());
            item.ifPresent(cartItemsRepository::delete);
            cartItemsRepository.flush();
        }

        logger.info("Removed item {} from cart session {}", variantSku, sessionToken);
        clearPricingSnapshot(session);
        return getCart(sessionToken);
    }

    @Override
    @Transactional
    public void clearCart(String sessionToken) {
        CartSession session = cartSessionsRepository.findByToken(sessionToken).orElse(null);
        if (session == null) {
            return;
        }

        // Release all stock reservations for this session
        stockReservationService.release(sessionToken);

        // Delete all cart items
        cartItemsRepository.deleteAllBySessionId(session.getId());
        cartItemsRepository.flush();

        clearPricingSnapshot(session);
        logger.info("Cleared cart session {}", sessionToken);
    }

    // ─── Validation ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CartItemPojo> validateCartStock(String sessionToken) {
        CartSession session = cartSessionsRepository.findByTokenDeep(sessionToken).orElse(null);
        if (session == null || session.getItems().isEmpty()) {
            return List.of();
        }

        List<CartItemPojo> unavailable = new ArrayList<>();
        for (CartItem item : session.getItems()) {
            ProductVariant variant = item.getVariant();
            Integer available = stockReservationService.getAvailableStock(variant.getSku());
            if (available == null || available < item.getQuantity() || !variant.isActive()) {
                unavailable.add(toItemPojo(item));
            }
        }
        return unavailable;
    }

    @Override
    @Transactional(readOnly = true)
    public void prepareForCheckout(String sessionToken) throws BadInputException {
        CartSession session = cartSessionsRepository.findByTokenDeep(sessionToken).orElse(null);
        if (session == null || session.getItems().isEmpty()) {
            throw new BadInputException("Cart is empty");
        }

        List<CartItemPojo> unavailable = validateCartStock(sessionToken);
        if (!unavailable.isEmpty()) {
            String unavailableSkus = unavailable.stream()
                .map(CartItemPojo::getVariantSkuResolved)
                .collect(Collectors.joining(", "));
            throw new BadInputException(
                "Some items are no longer available: " + unavailableSkus);
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private void clearPricingSnapshot(CartSession session) {
        if (session == null) {
            return;
        }
        session.setAppliedDiscountCode(null);
        session.setDiscountAmount(null);
        session.setAppliedPromotionsJson(null);
        cartSessionsRepository.saveAndFlush(session);
    }

    private CartSession getOrCreateCartSession(String sessionToken) {
        return cartSessionsRepository.findByToken(sessionToken)
            .orElseGet(() -> createNewSession(sessionToken));
    }

    private ProductVariant findActiveVariant(String sku) throws BadInputException {
        ProductVariant variant = productVariantsRepository.findBySku(sku)
            .orElseThrow(() -> new BadInputException("Variant not found: " + sku));
        if (!variant.isActive()) {
            throw new BadInputException("Variant is not active: " + sku);
        }
        return variant;
    }

    private CartSessionPojo toSessionPojo(CartSession session) {
        List<CartItemPojo> itemPojos = new ArrayList<>();
        int subtotal = 0;
        int totalUnits = 0;

        for (CartItem item : session.getItems()) {
            CartItemPojo itemPojo = toItemPojo(item);
            itemPojos.add(itemPojo);
            if (itemPojo.getLineTotal() != null) {
                subtotal += itemPojo.getLineTotal();
            }
            totalUnits += item.getQuantity();
        }

        Integer discount = session.getDiscountAmount();
        int afterDiscount = Math.max(0, subtotal - (discount != null ? discount : 0));

        return CartSessionPojo.builder()
            .id(session.getId())
            .token(session.getToken())
            .items(itemPojos)
            .subtotal(subtotal)
            .itemCount(itemPojos.size())
            .totalUnits(totalUnits)
            .appliedDiscountCode(session.getAppliedDiscountCode())
            .discountAmount(discount)
            .totalAfterDiscount(afterDiscount)
            .appliedPromotionsJson(session.getAppliedPromotionsJson())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .build();
    }

    private CartItemPojo toItemPojo(CartItem item) {
        ProductVariant variant = item.getVariant();
        CartItemPojo pojo = cartItemsConverterService.convertToPojo(item);

        Integer availableStock = null;
        if (variant != null) {
            availableStock = stockReservationService.getAvailableStock(variant.getSku());
        }
        boolean inStock = availableStock != null && availableStock >= item.getQuantity();
        boolean active = variant != null && variant.isActive();

        pojo.setProductBasePrice(variant != null && variant.getProduct() != null ? variant.getProduct().getPrice() : null);
        pojo.setAvailableStock(availableStock);
        pojo.setInStock(inStock);
        pojo.setActive(active);

        return pojo;
    }
}
