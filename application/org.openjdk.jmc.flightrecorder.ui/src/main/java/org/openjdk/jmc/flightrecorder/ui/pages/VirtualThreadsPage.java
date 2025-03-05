/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.flightrecorder.ui.pages;

import java.util.Arrays;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class VirtualThreadsPage extends AbstractDataPage {
	private static final ItemListBuilder LIST = new ItemListBuilder();

	public static class VirtualThreadsPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return "Virtual Threads";
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_THREADS);
		}

		@Override
		public String[] getTopics(IState state) {
			// TODO: add rules for virtual threads
			return new String[] { JfrRuleTopics.THREADS };
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new VirtualThreadsPage(dpd, items, editor);
		}

	}

	private static final IItemFilter TABLE_ITEMS = ItemFilters.or(JdkFilters.VIRTUAL_THREAD_START,
			JdkFilters.VIRTUAL_THREAD_END, JdkFilters.VIRTUAL_THREAD_PINNED);

	static {
		LIST.addColumn(JdkAttributes.EVENT_THREAD_ID);
		LIST.addColumn(JfrAttributes.EVENT_THREAD);
		LIST.addColumn(JfrAttributes.START_TIME);
	}

	private class VirtualThreadsPageUi implements IPageUI {
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
		private static final String LIST_ELEMENT = "eventList0"; //$NON-NLS-1$
		private static final String LIST_FILTER_ELEMENT = "eventListFilter"; //$NON-NLS-1$

		private final SashForm sash;
		private CTabFolder tabFolder;
		private final ItemList itemList;
		private FilterComponent itemFilter;
		private IItemFilter itemListFilter;

		VirtualThreadsPageUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());

			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);

			tabFolder = new CTabFolder(sash, SWT.NONE);
			toolkit.adapt(tabFolder);

			itemList = LIST.buildWithoutBorder(tabFolder, getTableSettings(state.getChild(LIST_ELEMENT)));
			MCContextMenuManager itemListMm = MCContextMenuManager
					.create(itemList.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(itemList.getManager(), itemListMm);

			itemList.getManager().setSelectionState(itemListSelection);
			itemList.show(getDataSource().getItems().apply(TABLE_ITEMS));
			itemFilter = FilterComponent.createFilterComponent(itemList, itemListFilter,
					getDataSource().getItems().apply(ItemFilters.or(JdkFilters.VIRTUAL_THREAD_START)),
					pageContainer.getSelectionStore()::getSelections, this::onEventsFilterChange);
			itemFilter.loadState(state.getChild(LIST_FILTER_ELEMENT));

			CTabItem t0 = new CTabItem(tabFolder, SWT.NONE);
			t0.setText("Virtual Thread Start");
			t0.setToolTipText("Showing all virtual thread start events");
			t0.setControl(itemFilter.getComponent());
			tabFolder.setSelection(0);
		}

		private void onEventsFilterChange(IItemFilter filter) {
			itemFilter.filterChangeHelper(filter, itemList, getDataSource().getItems().apply(JdkFilters.VIRTUAL_THREAD_START));
			itemListFilter = filter;
		}

		@Override
		public void saveTo(IWritableState writableState) {
			PersistableSashForm.saveState(sash, writableState.createChild(SASH_ELEMENT));
			itemList.getManager().getSettings().saveState(writableState.createChild(LIST_ELEMENT));
			saveToLocal();
		}

		private void saveToLocal() {
			itemListSelection = itemList.getManager().getSelectionState();
		}
	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(JdkAttributes.EVENT_THREAD_ID.getIdentifier(),
					Arrays.asList(
							new ColumnSettings(JdkAttributes.EVENT_THREAD_ID.getIdentifier(), false, 120, false),
							new ColumnSettings(JfrAttributes.EVENT_THREAD.getIdentifier(), false, 120, false),
							new ColumnSettings(JfrAttributes.START_TIME.getIdentifier(), false, 120, false)
					));
		} else {
			return new TableSettings(state);
		}
	}

	private SelectionState itemListSelection;

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new VirtualThreadsPageUi(parent, toolkit, editor, state);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return TABLE_ITEMS;
	}

	public VirtualThreadsPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

}
