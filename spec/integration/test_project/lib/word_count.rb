# encoding: utf-8

module WordCount
  class Mapper
    def initialize
      @text = Hadoop::Io::Text.new
      @one = Hadoop::Io::IntWritable.new(1)
    end

    def setup(ctx)
      ctx.get_counter('Setup and Cleanup', 'MAPPER_SETUP_COUNT').increment(1)
    end

    def cleanup(ctx)
      ctx.get_counter('Setup and Cleanup', 'MAPPER_CLEANUP_COUNT').increment(1)
    end

    def map(key, value, context)
      value.to_s.split.each do |word|
        word.downcase!
        word.gsub!(/\W/, '')
        unless word.empty?
          @text.set(word)
          context.write(@text, @one)
        end
      end
    end
  end

  class Reducer
    def initialize
      @output_value = Hadoop::Io::IntWritable.new
    end

    def setup(ctx)
      ctx.get_counter('Setup and Cleanup', 'REDUCER_SETUP_COUNT').increment(1)
    end

    def cleanup(ctx)
      ctx.get_counter('Setup and Cleanup', 'REDUCER_CLEANUP_COUNT').increment(1)
    end

    def reduce(key, values, context)
      total_sum = values.reduce(0) do |sum, value|
        sum + value.get
      end
      @output_value.set(total_sum)
      context.write(key, @output_value)
    end
  end

  class AliceDoublingCombiner < Reducer
    def reduce(key, values, context)
      if key.to_s == 'alice'
        total_sum = values.reduce(0) do |sum, value|
          sum + value.get
        end
        @output_value.set(total_sum * 2)
        context.write(key, @output_value)
      else
        values.each do |value|
          context.write(key, value)
        end
      end
    end

    def setup(ctx)
      ctx.get_counter('Setup and Cleanup', 'COMBINER_SETUP_COUNT').increment(1)
    end

    def cleanup(ctx)
      ctx.get_counter('Setup and Cleanup', 'COMBINER_CLEANUP_COUNT').increment(1)
    end
  end
end
