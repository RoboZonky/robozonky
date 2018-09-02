/*
 * Copyright 2018 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This code handles the integration with <a href="https://www.facebook.com/groups/204405453512737/about/">Stonky</a>.
 *
 * Stonky is a Google Spreadsheets add-on that analyzes Zonky XLS exports and provides a lot of very interesting data
 * about the portfolio. In order to obtain the add-on, we first need to clone the master Stonky spreadsheet into the
 * user's Google Drive.
 *
 * In order to have the add-on analyse our portfolio, we must provide it with Zonky data. In order to do that, we first
 * need to obtain those XLS exports (Wallet, Investments) from Zonky and then import them as sheets into the Stonky
 * spreadsheet copy from above. The spreadsheets are named "Wallet" and "People" respectively.
 *
 * We also upload a third sheet, named "Welcome". Stonky detects the presence of this sheet and notifies user that they
 * should run the analysis to see the latest financial data. During this analysis, the Welcome sheet will be deleted
 * automatically. Therefore the presence of the sheet indicates to the user that they have not yet seen this iteration
 * of the data.
 */
package com.github.robozonky.integrations.stonky;
