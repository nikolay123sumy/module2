import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShoppingCartTest {
    public static class AppendFormattedTest {
        private static Stream<Arguments> fixtures() {
            return Stream.of(
                    // в кінці строки додається пробіл
                    Arguments.of("string", -1, 3, "str "),  // перевірка обрізання
                    Arguments.of("str", -1, 7, "str     "), // вировнювання ліворуч
                    Arguments.of("str", 0, 7, "  str   "), // середина
                    Arguments.of("str", 1, 7, "    str ")  // праворуч
            );
        }

        @ParameterizedTest
        @MethodSource("fixtures")
        void testAppendFormatted(String value, int align, int width, String expected) {
            var sb = new StringBuilder();
            ShoppingCart.appendFormatted(sb, value, align, width);
            Assertions.assertEquals(expected, sb.toString());
        }
    }

    public static class CalculateDiscountTest {
        private static Stream<Arguments> fixtures() {
            return Stream.of(
                    // For NEW item discount is 0% (також перевіряємо, що 1% не враховується)
                    Arguments.of(ShoppingCart.ItemType.NEW, 100, 0),
                    // For SECOND_FREE item discount is 50% if quantity > 1
                    Arguments.of(ShoppingCart.ItemType.SECOND_FREE, 1, 0),
                    Arguments.of(ShoppingCart.ItemType.SECOND_FREE, 2, 50),
                    // For SALE item discount is 70%
                    Arguments.of(ShoppingCart.ItemType.SALE, 1, 70),
                    // For each full 10 not NEW items item gets additional 1% discount, but not more than 80% total
                    Arguments.of(ShoppingCart.ItemType.SECOND_FREE, 10, 51), // переконуємося, що +1% працює для SECOND_FREE
                    Arguments.of(ShoppingCart.ItemType.REGULAR, 10, 1), // для регулярних товарів
                    Arguments.of(ShoppingCart.ItemType.REGULAR, 19, 1),
                    Arguments.of(ShoppingCart.ItemType.REGULAR, 20, 2),
                    Arguments.of(ShoppingCart.ItemType.REGULAR, 21, 2),
                    Arguments.of(ShoppingCart.ItemType.REGULAR, 1000, 80), // максимум 80% знижки
                    Arguments.of(ShoppingCart.ItemType.SALE, 10, 71) // також +1% працює для SALE
            );
        }

        @ParameterizedTest
        @MethodSource("fixtures")
        void testCalculateDiscount(ShoppingCart.ItemType type, int quantity, int expected) {
            Assertions.assertEquals(expected, ShoppingCart.calculateDiscount(type, quantity));
        }
    }

    public static class AddItemTest {
        private ShoppingCart cart;

        @BeforeEach
        public void createCart() {
            cart = new ShoppingCart();
        }

        // назва товару повинна бути не пустою і не перевищувати 32 символи
        @Test
        public void titleLengthTest() {
            // пуста назва товару
            assertThrows(IllegalArgumentException.class, () -> {
                cart.addItem("", 1.00, 1, ShoppingCart.ItemType.REGULAR);
            });
            // длиніша ніж 32 символи
            assertThrows(IllegalArgumentException.class, () -> {
                cart.addItem("123456789012345678901234567890123", 1.00, 1, ShoppingCart.ItemType.REGULAR);
            });
            // в межах
            assertDoesNotThrow(() -> {
                cart.addItem("Title", 1.00, 1, ShoppingCart.ItemType.REGULAR);
            });
        }

        // перевірка значень ціни за межами та в межах діапазону
        @Test
        public void priceRangeTest() {
            // ціна повинна бути більшою нуля
            assertThrows(IllegalArgumentException.class, () -> {
                cart.addItem("Title", 0.00, 1, ShoppingCart.ItemType.REGULAR);
            });
            // ціна повинна бути в межах діапазону
            assertDoesNotThrow(() -> {
                cart.addItem("Title", 0.01, 1, ShoppingCart.ItemType.REGULAR);
                cart.addItem("Title", 1000.0, 1, ShoppingCart.ItemType.REGULAR);
            });
        }



        // кількість товару повинна бути більшою нуля, але не більшою 1000
        @Test
        public void quantityRangeTest() {
            assertThrows(IllegalArgumentException.class, () -> {
                cart.addItem("Title", 1.00, 0, ShoppingCart.ItemType.REGULAR);
            });
            assertDoesNotThrow(() -> {
                cart.addItem("Title", 1.00, 1, ShoppingCart.ItemType.REGULAR);
                cart.addItem("Title", 1.00, 1000, ShoppingCart.ItemType.REGULAR);
            });
        }
    }

    public static class FormatTicketTest {
        private ShoppingCart cart;

        // допоміжний клас для тестування
        private static class Item {
            String title;
            double price;
            int quantity;
            ShoppingCart.ItemType type;

            public Item(String title, double price, ShoppingCart.ItemType type, int quantity) {
                this.title = title;
                this.price = price;
                this.type = type;
                this.quantity = quantity;
            }
        }

        @BeforeEach
        public void createCart() {
            cart = new ShoppingCart();
        }

        private static Stream<Arguments> fixtures() {
            return Stream.of(
                    Arguments.of(new Item[]{}, "No items."),
                    Arguments.of(new Item[]{
                            new Item("Some title", 0.3, ShoppingCart.ItemType.REGULAR, 2)
                    }, """
                            # Item       Price Quan. Discount Total\s
                            ---------------------------------------
                            1 Some title  $.30     2        -  $.60\s
                            ---------------------------------------
                            1                                  $.60\s"""),
                    Arguments.of(new Item[]{
                            new Item("Some title", 0.3, ShoppingCart.ItemType.REGULAR, 2),
                            new Item("Some very long ti...", 100, ShoppingCart.ItemType.NEW, 2)
                    }, """
                            # Item                   Price Quan. Discount   Total\s
                            -----------------------------------------------------
                            1 Some title              $.30     2        -    $.60\s
                            2 Some very long ti... $100.00     2        - $200.00\s
                            -----------------------------------------------------
                            2                                             $200.60\s"""),
                    Arguments.of(new Item[]{
                            new Item("Some title", 0.3, ShoppingCart.ItemType.REGULAR, 2),
                            new Item("Some very long ti...", 100, ShoppingCart.ItemType.SALE, 2),
                            new Item("Item 42", 999.0, ShoppingCart.ItemType.REGULAR, 1000)
                    }, """
                            # Item                   Price Quan. Discount      Total\s
                            --------------------------------------------------------
                            1 Some title              $.30     2        -       $.60\s
                            2 Some very long ti... $100.00     2      70%     $60.00\s
                            3 Item 42              $999.00  1000      80% $199800.00\s
                            --------------------------------------------------------
                            3                                             $199860.60\s""")
            );
        }

        @ParameterizedTest
        @MethodSource("fixtures")
        void testToString(Item[] items, String expected) {
            for (Item item : items) {
                cart.addItem(item.title, item.price, item.quantity, item.type);
            }
            Assertions.assertEquals(expected, cart.formatTicket());
        }
    }
}
