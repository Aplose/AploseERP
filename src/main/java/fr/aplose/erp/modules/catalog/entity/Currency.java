package fr.aplose.erp.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
public class Currency {

    @Id
    @Column(name = "code", length = 3, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "symbol", length = 10, nullable = false)
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    private short decimalPlaces = 2;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public String getDisplayName() {
        return code + " - " + name + " (" + symbol + ")";
    }
}
