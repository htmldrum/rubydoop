package rubydoop;


import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Job;

import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.PathType;
import org.jruby.embed.InvokeFailedException;


public class RubydoopJobRunner extends Configured implements Tool {
    public int run(String[] args) throws Exception {
        String jobSetupScript = args[0];
        String[] arguments = Arrays.copyOfRange(args, 1, args.length);

        for (Job job : configureJobs(jobSetupScript, arguments)) {
            if (!job.waitForCompletion(true)) {
                return 1;
            }
        }

        return 0;
    }

    private List<Job> configureJobs(String jobSetupScript, String[] arguments) throws Exception {
        ScriptingContainer runtime = InstanceContainer.getRuntime();
        Configuration conf = getConf();
        conf.set(InstanceContainer.JOB_SETUP_SCRIPT_KEY, jobSetupScript);
        Object contextClass = runtime.runScriptlet("Rubydoop::Context");
        Object context = runtime.callMethod(contextClass, "new", conf, arguments);
        runtime.put("$rubydoop_context", context);

        try {
            runtime.callMethod(null, "require", jobSetupScript);
        } catch (InvokeFailedException e) {
            String message = String.format("Could not load job setup script (\"%s\"): \"%s\"", jobSetupScript, e.getMessage());
            throw new RubydoopRunnerException(message, e);
        }
        
        List<Job> jobs = (List<Job>) runtime.callMethod(context, "jobs");

        for (Job job : jobs) {
            job.setJarByClass(getClass());
        }

        return jobs;
    }

    public static void main(String[] args) throws Exception {
        System.exit(ToolRunner.run(new RubydoopJobRunner(), args));
    }
}
