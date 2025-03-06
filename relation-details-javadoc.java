package pl.santander.clp.product.common.relations;

import pl.santander.clp.product.common.relations.snapshot.ProductRelationSnapshot;
import java.util.Set;

/**
 * Interface defining operations for working with product relations.
 * <p>
 * This interface provides methods to query different types of relationships between products,
 * including modification, activation, renewal, resignation, refinancing, and absorption.
 * Each product can be either a source or target in these relationships.
 * </p>
 * <p>
 * The relationship types are divided into two categories:
 * <ul>
 *   <li>Inclusive relation types: ABSORPTION, REFINANCING</li>
 *   <li>Lifecycle relation types: ACTIVATION, MODIFICATION, CORRECTION, RENEWAL, RESIGNATION, etc.</li>
 * </ul>
 * </p>
 */
public interface RelationDetails {

    /**
     * Returns the ID of the product this relation details object represents.
     *
     * @return the product ID
     */
    Long getProductId();

    /**
     * Returns the set of all relations associated with this product.
     *
     * @return a set of {@link ProductRelationSnapshot} objects
     */
    Set<ProductRelationSnapshot> relations();

    /**
     * Checks if this product is the target of a MODIFICATION relation.
     * <p>
     * A product is considered modified when it is the target of a MODIFICATION relation.
     * </p>
     *
     * @return true if this product is modified, false otherwise
     */
    boolean isModification();

    /**
     * Checks if this product is the source of a MODIFICATION relation.
     * <p>
     * A product is a modification source when it initiated a modification
     * that resulted in another product.
     * </p>
     *
     * @return true if this product is a source of modification, false otherwise
     */
    boolean isModificationSourceProduct();

    /**
     * Returns the ID of the product that was the source of the MODIFICATION relation
     * where this product is the target.
     * <p>
     * This returns the ID of the product that this product was modified from.
     * </p>
     *
     * @return the ID of the source product, or null if this product is not a modified product
     */
    Long getModifiedProductId();

    /**
     * Checks if this product is the target of an ACTIVATION relation.
     * <p>
     * A product is considered activated when it is the target of an ACTIVATION relation.
     * </p>
     *
     * @return true if this product is activated, false otherwise
     */
    boolean isActivation();

    /**
     * Checks if this product is the source of an ACTIVATION relation.
     * <p>
     * A product is an activation source when it initiated an activation
     * that resulted in another product.
     * </p>
     *
     * @return true if this product is a source of activation, false otherwise
     */
    boolean isActivationSourceProduct();

    /**
     * Returns the ID of the product that was the source of the ACTIVATION relation
     * where this product is the target.
     * <p>
     * This returns the ID of the product that this product was activated from.
     * </p>
     *
     * @return the ID of the source product, or null if this product is not an activated product
     */
    Long getActivatedProductId();

    /**
     * Checks if this product is the target of a RENEWAL relation.
     * <p>
     * A product is considered renewed when it is the target of a RENEWAL relation.
     * </p>
     *
     * @return true if this product is renewed, false otherwise
     */
    boolean isRenewal();

    /**
     * Checks if this product is the source of a RENEWAL relation.
     * <p>
     * A product is a renewal source when it initiated a renewal
     * that resulted in another product.
     * </p>
     *
     * @return true if this product is a source of renewal, false otherwise
     */
    boolean isRenewalSourceProduct();

    /**
     * Returns the ID of the product that was the source of the RENEWAL relation
     * where this product is the target.
     * <p>
     * This returns the ID of the product that this product was renewed from.
     * </p>
     *
     * @return the ID of the source product, or null if this product is not a renewed product
     */
    Long getRenewedProductId();

    /**
     * Checks if this product is the target of a RESIGNATION relation.
     * <p>
     * A product is considered resigned when it is the target of a RESIGNATION relation.
     * </p>
     *
     * @return true if this product is resigned, false otherwise
     */
    boolean isResignation();

    /**
     * Checks if this product is the source of a RESIGNATION relation.
     * <p>
     * A product is a resignation source when it initiated a resignation
     * that resulted in another product.
     * </p>
     *
     * @return true if this product is a source of resignation, false otherwise
     */
    boolean isResignationSourceProduct();

    /**
     * Returns the ID of the product that was the source of the RESIGNATION relation
     * where this product is the target.
     * <p>
     * This returns the ID of the product that this product was resigned from.
     * </p>
     *
     * @return the ID of the source product, or null if this product is not a resigned product
     */
    Long getResignedProductId();

    /**
     * Checks if this product is refinanced by the specified product.
     * <p>
     * This product is considered to be refinanced by another product when:
     * <ul>
     *   <li>This product is the source of a REFINANCING relation</li>
     *   <li>The specified product is the target of the same REFINANCING relation</li>
     * </ul>
     * </p>
     *
     * @param productId the ID of the product to check against
     * @return true if this product is refinanced by the specified product, false otherwise
     */
    boolean isRefinancedBy(Long productId);

    /**
     * Checks if this product refinances the specified product.
     * <p>
     * This product is considered to refinance another product when:
     * <ul>
     *   <li>This product is the target of a REFINANCING relation</li>
     *   <li>The specified product is the source of the same REFINANCING relation</li>
     * </ul>
     * </p>
     *
     * @param productId the ID of the product to check against
     * @return true if this product refinances the specified product, false otherwise
     */
    boolean doesRefinance(Long productId);

    /**
     * Checks if this product is absorbed by the specified product.
     * <p>
     * This product is considered to be absorbed by another product when:
     * <ul>
     *   <li>This product is the source of an ABSORPTION relation</li>
     *   <li>The specified product is the target of the same ABSORPTION relation</li>
     * </ul>
     * </p>
     *
     * @param productId the ID of the product to check against
     * @return true if this product is absorbed by the specified product, false otherwise
     */
    boolean isAbsorbedBy(Long productId);

    /**
     * Checks if this product absorbs the specified product.
     * <p>
     * This product is considered to absorb another product when:
     * <ul>
     *   <li>This product is the target of an ABSORPTION relation</li>
     *   <li>The specified product is the source of the same ABSORPTION relation</li>
     * </ul>
     * </p>
     *
     * @param productId the ID of the product to check against
     * @return true if this product absorbs the specified product, false otherwise
     */
    boolean doesAbsorb(Long productId);
}