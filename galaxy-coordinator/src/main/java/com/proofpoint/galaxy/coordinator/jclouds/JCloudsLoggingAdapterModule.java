package com.proofpoint.galaxy.coordinator.jclouds;

import org.jclouds.logging.BaseLogger;
import org.jclouds.logging.Logger;
import org.jclouds.logging.Logger.LoggerFactory;
import org.jclouds.logging.config.LoggingModule;

public class JCloudsLoggingAdapterModule extends LoggingModule
{
    @Override
    public LoggerFactory createLoggerFactory()
    {
        return new LoggerFactory()
        {
            @Override
            public Logger getLogger(String category)
            {
                return new JCloudsAdaptedLogger(category);
            }
        };
    }

    public class JCloudsAdaptedLogger extends BaseLogger {

        private final com.proofpoint.log.Logger proofpointLogger;
        private final String category;

        public JCloudsAdaptedLogger(String category) {
            this.category = category;
            this.proofpointLogger = com.proofpoint.log.Logger.get(category);
        }

        @Override
        protected void logError(String message, Throwable e)
        {
            proofpointLogger.error(e, "%s", message);
        }

        @Override
        protected void logError(String message)
        {
            proofpointLogger.error("%s", message);
        }

        @Override
        protected void logWarn(String message, Throwable e)
        {
            proofpointLogger.warn(e, "%s", message);
        }

        @Override
        protected void logWarn(String message)
        {
            proofpointLogger.warn("%s", message);
        }

        @Override
        protected void logInfo(String message)
        {
            proofpointLogger.info("%s", message);
        }

        @Override
        protected void logDebug(String message)
        {
            proofpointLogger.debug("%s", message);
        }

        @Override
        protected void logTrace(String message)
        {
            proofpointLogger.debug("%s", message);
        }

        @Override
        public String getCategory()
        {
            return category;
        }

        @Override
        public boolean isTraceEnabled()
        {
            return proofpointLogger.isDebugEnabled();
        }

        @Override
        public boolean isDebugEnabled()
        {
            return proofpointLogger.isDebugEnabled();
        }

        @Override
        public boolean isInfoEnabled()
        {
            return proofpointLogger.isInfoEnabled();
        }

        @Override
        public boolean isWarnEnabled()
        {
            return true;
        }

        @Override
        public boolean isErrorEnabled()
        {
            return true;
        }
    }

}
