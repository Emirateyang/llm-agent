package com.llmagent.llm.service.output;

import java.math.BigInteger;

public class BigIntegerOutputParser implements OutputParser<BigInteger> {

    @Override
    public BigInteger parse(String string) {
        return new BigInteger(string.trim());
    }

    @Override
    public String formatInstructions() {
        return "integer number";
    }
}
