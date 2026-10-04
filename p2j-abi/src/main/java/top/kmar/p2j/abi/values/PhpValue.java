package top.kmar.p2j.abi.values;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * PHP 运行时值的包装器。
 *
 * <p>PhpValue 表示一个可变的 PHP 值单元，负责维护 PHP 的值类型以及赋值、
 * 引用等基础语义。普通赋值会复制 PhpValue，而引用赋值会让多个变量共享
 * 同一个 PhpValue 实例。
 *
 * <p>复杂类型的具体存储和行为由对应类型自身实现。例如 PHP Array 的
 * Copy-On-Write 由 PhpArray 负责，PhpValue 仅负责在赋值时复制相应的
 * PhpArray 句柄。
 */
public final class PhpValue {

    private Object value;

    /**
     * 按 PHP 7.2 的 {@code <=>} 规则比较，结果不保证满足 Java 排序比较器的契约。
     */
    public int compare(PhpValue other) {
        Objects.requireNonNull(other, "other");
        Object left = value;
        Object right = other.value;

        // int 与 int：精确比较 64 位整数，不转为浮点数。
        if (left instanceof Long a && right instanceof Long b) {
            return Long.compare(a, b);
        }
        // float 与 float：使用 PHP 的浮点比较规则，包含 NaN、无穷大和正负零。
        if (left instanceof Double a && right instanceof Double b) {
            return compareDoubles(a, b);
        }
        // string 与 string：两边均为完整数字字符串时走数字字符串比较（含溢出处理），否则按字节比较。
        if (left instanceof PhpString a && right instanceof PhpString b) {
            return a.compareLoose(b);
        }

        // 左 null、右 string：只与空字符串相等，小于任何非空字符串；优先于通用布尔转换。
        if (left == null && right instanceof PhpString b) {
            return b.isEmpty() ? 0 : -1;
        }
        // 左 string、右 null：空字符串相等，非空字符串更大。
        if (right == null && left instanceof PhpString a) {
            return a.isEmpty() ? 0 : 1;
        }
        // 左 null、右 object：null 小于对象，优先于对象的转换或自定义比较。
        if (left == null && right instanceof PhpObject) {
            return -1;
        }
        // 左 object、右 null：对象大于 null。
        if (right == null && left instanceof PhpObject) {
            return 1;
        }
        // 剩余涉及 object 的组合：委托对象比较或转换流程，保持左右顺序；混合类型暂未实现。
        if (left instanceof PhpObject || right instanceof PhpObject) {
            return compareObjects(left, right);
        }

        // 排除上述特殊组合后，任意一边为 null 或 bool：两边转布尔值比较，false < true。
        if (left == null || right == null
                || left instanceof Boolean || right instanceof Boolean) {
            return Boolean.compare(toBoolean(left), toBoolean(right));
        }
        // array 与 array：委托数组比较，先比元素数量，再按键查找并递归比较值。
        if (left instanceof PhpArray a && right instanceof PhpArray b) {
            return a.compare(b);
        }
        // 左 array、右 int/float/string/resource：数组更大，包括空数组。
        if (left instanceof PhpArray) {
            return 1;
        }
        // 左 int/float/string/resource、右 array：左边更小。
        if (right instanceof PhpArray) {
            return -1;
        }
        // 剩余合法组合为 int/float/string/resource，包含 int 与 float 的混合比较。
        // 字符串取数字前缀，无数字前缀则为整数零；资源取整数 ID。
        // 转换后两边均为整数时精确比较，否则按浮点数比较。
        return compareNumeric(left, right);
    }

    private static boolean toBoolean(@Nullable Object operand) {
        return switch (operand) {
            case null -> false;
            case Boolean b -> b;
            case Long n -> n != 0;
            case Double n -> n != 0.0;
            case PhpString s -> s.toBoolean();
            case PhpArray a -> a.toBoolean();
            case PhpResource r -> r.toBoolean();
            default -> throw unsupportedType(operand);
        };
    }

    private static int compareNumeric(@Nullable Object left, @Nullable Object right) {
        if (isIntegerNumeric(left) && isIntegerNumeric(right)) {
            return Long.compare(numericLong(left), numericLong(right));
        }
        return compareDoubles(numericDouble(left), numericDouble(right));
    }

    private static boolean isIntegerNumeric(@Nullable Object operand) {
        return switch (operand) {
            case Long ignored -> true;
            case Double ignored -> false;
            case PhpResource ignored -> true;
            case PhpString s -> s.isIntegerNumeric();
            case null, default -> throw unsupportedType(operand);
        };
    }

    private static long numericLong(@Nullable Object operand) {
        return switch (operand) {
            case Long n -> n;
            case PhpString s -> s.toLong();
            case PhpResource r -> r.toLong();
            case null, default -> throw unsupportedType(operand);
        };
    }

    private static double numericDouble(@Nullable Object operand) {
        return switch (operand) {
            case Long n -> n;
            case Double n -> n;
            case PhpString s -> s.toDouble();
            case PhpResource r -> r.toDouble();
            case null, default -> throw unsupportedType(operand);
        };
    }

    private static int compareDoubles(double left, double right) {
        if (left == right) {
            return 0;
        }
        double difference = left - right;
        // PHP 的差值归一化在 difference 为 NaN 时返回 1。
        //noinspection UseCompareMethod
        return difference < 0.0 ? -1 : difference == 0.0 ? 0 : 1;
    }

    private static int compareObjects(@Nullable Object left, @Nullable Object right) {
        if (left instanceof PhpObject a && right instanceof PhpObject b) {
            return a.compare(b);
        }
        // TODO: 实现对象处理器及对象与其他类型比较时的转换规则。
        throw new UnsupportedOperationException("PHP object comparison with other types is not implemented");
    }

    private static IllegalStateException unsupportedType(@Nullable Object operand) {
        return new IllegalStateException("Unsupported PHP value type: "
                + (operand == null ? "null" : operand.getClass().getName()));
    }

    @Override
    public String toString() {
        if (value == null) {
            return "";
        } if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "";
        } else {
            return value.toString();
        }
    }

}