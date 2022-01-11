import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ShoppingCartTest {
    public static class AppendFormattedTest {
        private static Stream<Arguments> fixtures() {
            return Stream.of(
                    // в кінці строки додається пробіл
                    Arguments.of("string", -1, 3, "str "),  // перевірка обрізання
                    Arguments.of("str", -1, 7, "str     "), // вировнювання ліворуч
                    Arguments.of("str",  0, 7, "  str   "), // середина
                    Arguments.of("str",  1, 7, "    str ")  // праворуч
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
}
