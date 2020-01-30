module com.github.robozonky.strategy.natural {
    requires org.antlr.antlr4.runtime;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;

    provides com.github.robozonky.api.strategies.StrategyService with com.github.robozonky.strategy.natural.NaturalLanguageStrategyService;
}
