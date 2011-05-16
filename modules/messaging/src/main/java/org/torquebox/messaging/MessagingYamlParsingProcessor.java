/*
 * Copyright 2008-2011 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.torquebox.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.torquebox.core.AbstractSplitYamlParsingProcessor;
import org.torquebox.core.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Creates ScheduledJobMetaData instances from jobs.yml
 */
public class MessagingYamlParsingProcessor extends AbstractSplitYamlParsingProcessor {

    public MessagingYamlParsingProcessor() {
        setSectionName( "messaging" );
    }

    public void parse(DeploymentUnit unit, Object dataObject) throws DeploymentUnitProcessingException {
        try {
            for (MessageProcessorMetaData metadata : Parser.parse( dataObject )) {
                unit.addToAttachmentList( MessageProcessorMetaData.ATTACHMENT_KEY, metadata );
            }
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException( e );
        }
    }

    public static class Parser {

        @SuppressWarnings("unchecked")
        static List<MessageProcessorMetaData> parse(Object data) throws Exception {
            if (data instanceof String) {
                String s = (String) data;
                if (s.trim().length() == 0) {
                    return Collections.emptyList();
                } else {
                    return parse( new Yaml().load( (String) data ) );
                }
            }
            return parseDestinations( (Map<String, Object>) data );
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        static List<MessageProcessorMetaData> parseDestinations(Map<String, Object> data) {
            List<MessageProcessorMetaData> result = new ArrayList<MessageProcessorMetaData>();
            for (String destination : data.keySet()) {
                Object value = data.get( destination );
                if (value instanceof Map) {
                    result.addAll( parseHandlers( destination, (Map<String, Map>) value ) );
                } else if (value instanceof List) {
                    result.addAll( parseHandlers( destination, (List) value ) );
                } else {
                    result.add( parseHandler( destination, (String) value ) );
                }
            }
            return result;
        }

        static MessageProcessorMetaData parseHandler(String destination, String handler) {
            return subscribe( handler, destination, Collections.EMPTY_MAP );
        }

        @SuppressWarnings("rawtypes")
        static List<MessageProcessorMetaData> parseHandlers(String destination, Map<String, Map> handlers) {
            List<MessageProcessorMetaData> result = new ArrayList<MessageProcessorMetaData>();
            for (String handler : handlers.keySet()) {
                result.add( subscribe( handler, destination, handlers.get( handler ) ) );
            }
            return result;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        static List<MessageProcessorMetaData> parseHandlers(String destination, List handlers) {
            List<MessageProcessorMetaData> result = new ArrayList<MessageProcessorMetaData>();
            for (Object v : handlers) {
                if (v instanceof String) {
                    result.add( parseHandler( destination, (String) v ) );
                } else {
                    result.addAll( parseHandlers( destination, (Map<String, Map>) v ) );
                }
            }
            return result;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static MessageProcessorMetaData subscribe(String handler, String destination, Map options) {
            if (options == null)
                options = Collections.EMPTY_MAP;
            MessageProcessorMetaData result = new MessageProcessorMetaData();
            result.setRubyClassName( StringUtils.camelize( handler ) );
            result.setRubyRequirePath( StringUtils.underscore( handler ) );
            result.setDestinationName( destination );
            result.setMessageSelector( (String) options.get( "filter" ) );
            result.setRubyConfig( (Map) options.get( "config" ) );
            result.setConcurrency( (Integer) options.get( "concurrency" ) );
            result.setDurable( (Boolean) options.get( "durable" ) );
            return result;
        }

    }

    private static final Logger log = Logger.getLogger( "org.torquebox.messaging" );
}
