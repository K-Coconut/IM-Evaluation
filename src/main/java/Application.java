import com.cityu.im.GenerateRRSets;
import com.cityu.im.InfluenceEvaluation;

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
        List<Integer> budgets = Arrays.asList(properties.getProperty("budgets").split(" ")).stream().map(Integer::parseInt).collect(Collectors.toList());
        String type = args[0];
        int num = 100000;
        if (args.length > 2) {
            num = Integer.parseInt(args[2]);
        }
        if (type.equals("gen")) {
            GenerateRRSets.generate(args[1], num, properties.getProperty("basePath"));
        } else if (type.equals("eval")) {
            InfluenceEvaluation.eval(args[1], num, properties.getProperty("basePath"), budgets);
        } else {
            throw new Exception("task type should be gen or eval");
        }

    }
}
