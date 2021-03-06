/*
 * Copyright 2013 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.otec.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Brock
 */
public class OTLogUtil {
  private static final Logger logger = LoggerFactory.getLogger(OTLogUtil.class);
  
  private static OTLogAdapter logAdapter = new OTLogAdapter() {
    @Override
    public void printLogTitle() {
    }

    /**
     * Logs an OT information to slf4j at the debug level.
     */
    @Override
    public boolean log(final String type,
                       final String mutations,
                       final String from,
                       final String to,
                       final int rev,
                       final String state) {

      logger.debug(type + ":" + mutations + ";rev=" + rev +";state=\"" + state + "\"");
      return true;
    }

    /**
     * Use {@link Logger} instead.
     */
    @Deprecated
    public boolean log(final String message) {
      logger.info(message);
      return true;
    }
  };

  public static void setLogAdapter(final OTLogAdapter logAdapter) {
    OTLogUtil.logAdapter = logAdapter;
  }

  public static void printLogTitle() {
    logAdapter.printLogTitle();
  }

  public static boolean log(final String type,
                            final String mutations,
                            final String from,
                            final String to,
                            final int rev,
                            final String state) {

    logAdapter.log(type, mutations, from, to, rev, state);
    return true;
  }

  public static boolean log(final String message) {
    logAdapter.log(message);
    return true;
  }
}
