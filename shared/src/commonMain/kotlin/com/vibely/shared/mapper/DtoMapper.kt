package com.vibely.shared.mapper

/**
 * Bidirectional mapper between a network [Dto] and a [Domain] model.
 *
 * [Dto] types are serialisation-optimised (annotated with `@Serializable`);
 * [Domain] types are framework-free. Mappers live in the data layer.
 *
 * @param Dto A serialisation-optimised struct for the network layer.
 * @param Domain A rich domain model with business logic and validation.
 */
interface DtoMapper<Dto, Domain> {
    /**
     * Converts a network [dto] to its [Domain] representation.
     */
    fun toDomain(dto: Dto): Domain

    /**
     * Converts a [domain] model to its network [Dto] representation.
     */
    fun toDto(domain: Domain): Dto
}
