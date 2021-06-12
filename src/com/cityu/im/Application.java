package com.cityu.im;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Ethan
 * @date 2021/6/12 3:30 下午
 */
public class Application {
    public static void main(String[] args) throws Exception {
        String type = args[0];
        int num = 100000;
        if (args.length > 2) {
            num = Integer.parseInt(args[2]);
        }
        if (type.equals("gen")) {
            GenerateRRSets.generate(args[1], num);
        } else if (type.equals("eval")) {
            InfluenceEvaluation.eval(args[1], num);
        } else {
            throw new Exception("task type should be gen or eval");
        }

    }
}
