package top.kmar.p2j.abi.values;

public class PhpString {

    /**
     * 标记指定缓存是否已经计算出来
     * 0 - longCache
     * 1 - doubleCache
     */
    private int cacheBitMap = 0;
    private long longCache = 0;

    public boolean isNumeric() {
        return false;
    }

    public boolean isEmpty() {
        // TODO: 根据底层字节长度判断。
        throw new UnsupportedOperationException("PHP string emptiness is not implemented");
    }

    public boolean toBoolean() {
        // TODO: 仅空字符串和单字节字符串 "0" 转换为 false。
        throw new UnsupportedOperationException("PHP string boolean conversion is not implemented");
    }

    /** 数字前缀解析结果是否为整数类型，包括无数字前缀时得到的整数零。 */
    public boolean isIntegerNumeric() {
        initNumCache();
        return (cacheBitMap & 0b1) != 0;
    }

    /** PHP 宽松比较，包含完整数字字符串判定和溢出回退规则。 */
    public int compareLoose(PhpString other) {
        // TODO: 实现 PHP 7.2 的数字字符串比较，否则调用原始字节比较。
        throw new UnsupportedOperationException("PHP loose string comparison is not implemented");
    }

    public long toLong() {
        initNumCache();
        if ((cacheBitMap & 0b1) != 0) {
            return longCache;
        } else if ((cacheBitMap & 0b10) != 0) {
            return (long) Double.longBitsToDouble(longCache);
        } else {
            throw new AssertionError();
        }
    }

    public double toDouble() {
        initNumCache();
        if ((cacheBitMap & 0b10) != 0) {
            return Double.longBitsToDouble(longCache);
        } else if ((cacheBitMap & 0b1) != 0) {
            return longCache;
        } else {
            throw new AssertionError();
        }
    }

    private void initNumCache() {
        if ((cacheBitMap & 0b11) != 0) {
            return;
        }
        cacheBitMap |= 0b1;
        longCache = 1;
    }

    public int compare(PhpString other) {
        return 0;
    }

}