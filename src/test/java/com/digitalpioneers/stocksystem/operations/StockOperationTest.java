package com.digitalpioneers.stocksystem.operations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.digitalpioneers.stocksystem.models.SaleItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockOperationTest {
    @Test
    void acceptsValidSaleLinesAndRestock() {
        Sale sale = new Sale(List.of(new SaleItem(1, 2), new SaleItem(2, 1)));
        StockRestock restock = new StockRestock();
        restock.productId = 1;
        restock.quantity = 8;

        assertDoesNotThrow(sale::validate);
        assertDoesNotThrow(restock::validate);
    }

    @Test
    void rejectsEmptySaleAndInvalidQuantities() {
        assertThrows(IllegalArgumentException.class, () -> new Sale(List.of()).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new Sale(List.of(new SaleItem(1, 0))).validate());
        assertThrows(IllegalArgumentException.class, () -> {
            StockRestock restock = new StockRestock();
            restock.productId = 1;
            restock.quantity = 0;
            restock.validate();
        });
    }

    @Test
    void rejectsOverflowingDuplicateProductQuantities() {
        Sale sale = new Sale(List.of(new SaleItem(1, Integer.MAX_VALUE), new SaleItem(1, 1)));
        assertThrows(IllegalArgumentException.class, sale::validate);
    }
}
