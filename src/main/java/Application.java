import com.cityu.im.GenerateRRSets;
import com.cityu.im.InfluenceEvaluation;

import java.awt.*;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Ethan
 * @date 2021/6/12 3:30 下午
 */
public class Application {
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        String confPath = System.getProperty("user.dir") + "/src/main/resources/conf.properties";
        properties.load(new FileInputStream(confPath));
//        List<Integer> budgets = Arrays.asList(properties.getProperty("budgets").split(" ")).stream().map(Integer::parseInt).collect(Collectors.toList());
        String type = args[0];

        int num = 100000;
        int nIter = 1;
        if (type.equals("gen")) {
            if (args.length > 2) {
                num = Integer.parseInt(args[2]);
            }
            String dataset = args[1];
            GenerateRRSets.generate(dataset, num, properties.getProperty("basePath"));
        } else if (type.equals("eval")) {
            String mode = args[1];
            if (!mode.equals("gcomb") && !mode.equals("gcomb_epoch") && !mode.equals("imm") && !mode.equals("DDiscount") && !mode.equals("SingleDiscount") && !mode.equals("interp_imm")) {
                throw new Exception("mode must be gcomb/imm");
            }
            String dataset = args[2];
            if (args.length > 3) {
                num = Integer.parseInt(args[3]);
            }
            if (args.length > 4) {
                nIter = Integer.parseInt(args[4]);
            }
            InfluenceEvaluation.eval(mode, dataset, num, nIter, properties.getProperty("basePath"));
        } else {
            throw new Exception("task type should be gen or eval");
        }

    }
}
