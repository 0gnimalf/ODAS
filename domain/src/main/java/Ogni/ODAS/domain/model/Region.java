package Ogni.ODAS.domain.model;

public record Region (
        Long id,
        String code,
        String name,
        String federalDistrict,
        boolean active
){
}
