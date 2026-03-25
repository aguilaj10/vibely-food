package com.vibely.shared.mapper

/**
 * Bidirectional mapper between a persistence [Entity] and a [Domain] model.
 *
 * Implementations live in the data layer; domain models never import [Entity] types.
 *
 * @param Entity A flat, persistence-optimised struct (e.g., Room entity, Exposed ResultRow mapping).
 * @param Domain A rich domain model with business logic and validation.
 */
interface DomainMapper<Entity, Domain> {

    /**
     * Converts a persistence [entity] to its [Domain] representation.
     */
    fun toDomain(entity: Entity): Domain

    /**
     * Converts a [domain] model to its persistence [Entity] representation.
     */
    fun toEntity(domain: Domain): Entity
}
