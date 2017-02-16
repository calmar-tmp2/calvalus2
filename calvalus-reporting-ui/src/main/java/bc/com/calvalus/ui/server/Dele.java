/*
package bc.com.calvalus.ui.server;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.AbstractHeaderOrFooterBuilder;
import com.google.gwt.user.client.ui.Widget;
import com.sun.corba.se.pept.transport.ContactInfo;

*/
/**
 * @author ubits on 2/15/2017.
 *//*

public class Dele {
}

*/
/**
 * The constants used in this Content Widget.
 *//*

 static interface CwConstants extends Constants {
    String cwCustomDataGridColumnAddress();

    String cwCustomDataGridColumnAge();

    String cwCustomDataGridColumnCategory();

    String cwCustomDataGridColumnFirstName();

    String cwCustomDataGridColumnLastName();

    String cwCustomDataGridDescription();

    String cwCustomDataGridEmpty();

    String cwCustomDataGridName();
}

*/
/**
 * The UiBinder interface used by this example.
 *//*

interface Binder extends UiBinder<Widget, CwCustomDataGrid> {
}

*/
/**
 * The resources used by this example.
 *//*

interface Resources extends ClientBundle {

    */
/**
     * Get the styles used but this example.
     *//*

    @Source("CwCustomDataGrid.css")
    Styles styles();
}

*/
/**
 * The CSS Resources used by this example.
 *//*

interface Styles extends CssResource {
    */
/**
     * Indents cells in child rows.
     *//*

    String childCell();

    */
/**
     * Applies to group headers.
     *//*

    String groupHeaderCell();
}


class CustomHeaderBuilder extends AbstractHeaderOrFooterBuilder<ContactInfo> {

    private Header<String> firstNameHeader = new TextHeader(constants
            .cwCustomDataGridColumnFirstName());
    private Header<String> lastNameHeader = new TextHeader(constants
            .cwCustomDataGridColumnLastName());
    private Header<String> ageHeader = new TextHeader(constants.cwCustomDataGridColumnAge());
    private Header<String> categoryHeader = new TextHeader(constants
            .cwCustomDataGridColumnCategory());
    private Header<String> addressHeader =
            new TextHeader(constants.cwCustomDataGridColumnAddress());

    public CustomHeaderBuilder() {
        super(dataGrid, false);
        setSortIconStartOfLine(false);
    }

    @Override
    protected boolean buildHeaderOrFooterImpl() {
        Style style = dataGrid.getResources().style();
        String groupHeaderCell = resources.styles().groupHeaderCell();

        // Add a 2x2 header above the checkbox and show friends columns.
        TableRowBuilder tr = startRow();
        tr.startTH().colSpan(2).rowSpan(2)
                .className(style.header() + " " + style.firstColumnHeader());
        tr.endTH();

      */
/*
       * Name group header. Associated with the last name column, so clicking on
       * the group header sorts by last name.
       *//*

        TableCellBuilder th = tr.startTH().colSpan(2).className(groupHeaderCell);
        enableColumnHandlers(th, lastNameColumn);
        th.style().trustedProperty("border-right", "10px solid white").cursor(Cursor.POINTER)
                .endStyle();
        th.text("Name").endTH();

        // Information group header.
        th = tr.startTH().colSpan(3).className(groupHeaderCell);
        th.text("Information").endTH();

        // Get information about the sorted column.
        ColumnSortList sortList = dataGrid.getColumnSortList();
        ColumnSortInfo sortedInfo = (sortList.size() == 0) ? null : sortList.get(0);
        Column<?, ?> sortedColumn = (sortedInfo == null) ? null : sortedInfo.getColumn();
        boolean isSortAscending = (sortedInfo == null) ? false : sortedInfo.isAscending();

        // Add column headers.
        tr = startRow();
        buildHeader(tr, firstNameHeader, firstNameColumn, sortedColumn, isSortAscending, false, false);
        buildHeader(tr, lastNameHeader, lastNameColumn, sortedColumn, isSortAscending, false, false);
        buildHeader(tr, ageHeader, ageColumn, sortedColumn, isSortAscending, false, false);
        buildHeader(tr, categoryHeader, categoryColumn, sortedColumn, isSortAscending, false, false);
        buildHeader(tr, addressHeader, addressColumn, sortedColumn, isSortAscending, false, true);
        tr.endTR();

        return true;
    }

    */
/**
     * Renders the header of one column, with the given options.
     *
     * @param out the table row to build into
     * @param header the {@link Header} to render
     * @param column the column to associate with the header
     * @param sortedColumn the column that is currently sorted
     * @param isSortAscending true if the sorted column is in ascending order
     * @param isFirst true if this the first column
     * @param isLast true if this the last column
     *//*

    private void buildHeader(TableRowBuilder out, Header<?> header, Column<ContactInfo, ?> column,
                             Column<?, ?> sortedColumn, boolean isSortAscending, boolean isFirst, boolean isLast) {
        // Choose the classes to include with the element.
        Style style = dataGrid.getResources().style();
        boolean isSorted = (sortedColumn == column);
        StringBuilder classesBuilder = new StringBuilder(style.header());
        if (isFirst) {
            classesBuilder.append(" " + style.firstColumnHeader());
        }
        if (isLast) {
            classesBuilder.append(" " + style.lastColumnHeader());
        }
        if (column.isSortable()) {
            classesBuilder.append(" " + style.sortableHeader());
        }
        if (isSorted) {
            classesBuilder.append(" "
                    + (isSortAscending ? style.sortedHeaderAscending() : style.sortedHeaderDescending()));
        }

        // Create the table cell.
        TableCellBuilder th = out.startTH().className(classesBuilder.toString());

        // Associate the cell with the column to enable sorting of the column.
        enableColumnHandlers(th, column);

        // Render the header.
        Context context = new Context(0, 2, header.getKey());
        renderSortableHeader(th, context, header, isSorted, isSortAscending);

        // End the table cell.
        th.endTH();
    }
}

*/
/**
 * Renders custom table footers that appear beneath the columns in the table.
 * This footer consists of a single cell containing the average age of all
 * contacts on the current page. This is an example of a dynamic footer that
 * changes with the row data in the table.
 *//*

private class CustomFooterBuilder extends AbstractHeaderOrFooterBuilder<ContactInfo> {

    public CustomFooterBuilder() {
        super(dataGrid, true);
    }

    @Override
    protected boolean buildHeaderOrFooterImpl() {
        String footerStyle = dataGrid.getResources().style().footer();

        // Calculate the age of all visible contacts.
        String ageStr = "";
        List<ContactInfo> items = dataGrid.getVisibleItems();
        if (items.size() > 0) {
            int totalAge = 0;
            for (ContactInfo item : items) {
                totalAge += item.getAge();
            }
            ageStr = "Avg: " + totalAge / items.size();
        }

        // Cells before age column.
        TableRowBuilder tr = startRow();
        tr.startTH().colSpan(4).className(footerStyle).endTH();

        // Show the average age of all contacts.
        TableCellBuilder th =
                tr.startTH().className(footerStyle).align(
                        HasHorizontalAlignment.ALIGN_CENTER.getTextAlignString());
        th.text(ageStr);
        th.endTH();

        // Cells after age column.
        tr.startTH().colSpan(2).className(footerStyle).endTH();
        tr.endTR();

        return true;
    }
}

*/
/**
 * Renders the data rows that display each contact in the table.
 *//*

private class CustomTableBuilder extends AbstractCellTableBuilder<ContactInfo> {

    private final int todayMonth;

    private final String childCell = " " + resources.styles().childCell();
    private final String rowStyle;
    private final String selectedRowStyle;
    private final String cellStyle;
    private final String selectedCellStyle;

    @SuppressWarnings("deprecation")
    public CustomTableBuilder() {
        super(dataGrid);

        // Cache styles for faster access.
        Style style = dataGrid.getResources().style();
        rowStyle = style.evenRow();
        selectedRowStyle = " " + style.selectedRow();
        cellStyle = style.cell() + " " + style.evenRowCell();
        selectedCellStyle = " " + style.selectedRowCell();

        // Record today's date.
        Date today = new Date();
        todayMonth = today.getMonth();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void buildRowImpl(ContactInfo rowValue, int absRowIndex) {
        buildContactRow(rowValue, absRowIndex, false);

        // Display information about the user in another row that spans the entire
        // table.
        Date dob = rowValue.getBirthday();
        if (dob.getMonth() == todayMonth) {
            TableRowBuilder row = startRow();
            TableCellBuilder td = row.startTD().colSpan(7).className(cellStyle);
            td.style().trustedBackgroundColor("#ccf").endStyle();
            td.text(rowValue.getFirstName() + "'s birthday is this month!").endTD();
            row.endTR();
        }

        // Display list of friends.
        if (showingFriends.contains(rowValue.getId())) {
            Set<ContactInfo> friends = ContactDatabase.get().queryFriends(rowValue);
            for (ContactInfo friend : friends) {
                buildContactRow(friend, absRowIndex, true);
            }
        }
    }

    */
/**
     * Build a row.
     *
     * @param rowValue the contact info
     * @param absRowIndex the absolute row index
     * @param isFriend true if this is a subrow, false if a top level row
     *//*

    @SuppressWarnings("deprecation")
    private void buildContactRow(ContactInfo rowValue, int absRowIndex, boolean isFriend) {
        // Calculate the row styles.
        SelectionModel<? super ContactInfo> selectionModel = dataGrid.getSelectionModel();
        boolean isSelected =
                (selectionModel == null || rowValue == null) ? false : selectionModel
                        .isSelected(rowValue);
        boolean isEven = absRowIndex % 2 == 0;
        StringBuilder trClasses = new StringBuilder(rowStyle);
        if (isSelected) {
            trClasses.append(selectedRowStyle);
        }

        // Calculate the cell styles.
        String cellStyles = cellStyle;
        if (isSelected) {
            cellStyles += selectedCellStyle;
        }
        if (isFriend) {
            cellStyles += childCell;
        }

        TableRowBuilder row = startRow();
        row.className(trClasses.toString());

      */
/*
       * Checkbox column.
       *
       * This table will uses a checkbox column for selection. Alternatively,
       * you can call dataGrid.setSelectionEnabled(true) to enable mouse
       * selection.
       *//*

        TableCellBuilder td = row.startTD();
        td.className(cellStyles);
        td.style().outlineStyle(OutlineStyle.NONE).endStyle();
        if (!isFriend) {
            renderCell(td, createContext(0), checkboxColumn, rowValue);
        }
        td.endTD();

      */
/*
       * View friends column.
       *
       * Displays a link to "show friends". When clicked, the list of friends is
       * displayed below the contact.
       *//*

        td = row.startTD();
        td.className(cellStyles);
        if (!isFriend) {
            td.style().outlineStyle(OutlineStyle.NONE).endStyle();
            renderCell(td, createContext(1), viewFriendsColumn, rowValue);
        }
        td.endTD();

        // First name column.
        td = row.startTD();
        td.className(cellStyles);
        td.style().outlineStyle(OutlineStyle.NONE).endStyle();
        if (isFriend) {
            td.text(rowValue.getFirstName());
        } else {
            renderCell(td, createContext(2), firstNameColumn, rowValue);
        }
        td.endTD();

        // Last name column.
        td = row.startTD();
        td.className(cellStyles);
        td.style().outlineStyle(OutlineStyle.NONE).endStyle();
        if (isFriend) {
            td.text(rowValue.getLastName());
        } else {
            renderCell(td, createContext(3), lastNameColumn, rowValue);
        }
        td.endTD();

        // Age column.
        td = row.startTD();
        td.className(cellStyles);
        td.style().outlineStyle(OutlineStyle.NONE).endStyle();
        td.text(NumberFormat.getDecimalFormat().format(rowValue.getAge())).endTD();

        // Category column.
        td = row.startTD();
        td.className(cellStyles);
        td.style().outlineStyle(OutlineStyle.NONE).endStyle();
        if (isFriend) {
            td.text(rowValue.getCategory().getDisplayName());
        } else {
            renderCell(td, createContext(5), categoryColumn, rowValue);
        }
        td.endTD();

        // Address column.
        td = row.startTD();
        td.className(cellStyles);
        DivBuilder div = td.startDiv();
        div.style().outlineStyle(OutlineStyle.NONE).endStyle();
        div.text(rowValue.getAddress()).endDiv();
        td.endTD();

        row.endTR();
    }
}

    */
/**
     * The main DataGrid.
     *//*

    @UiField(provided = true)
    DataGrid<ContactInfo> dataGrid;

    */
/**
     * The pager used to change the range of data.
     *//*

    @UiField(provided = true)
    SimplePager pager;

    */
/**
     * An instance of the constants.
     *//*

    private final CwConstants constants;

    */
/**
     * The resources used by this example.
     *//*

    private Resources resources;

    */
/**
     * Contains the contact id for each row in the table where the friends list is
     * currently expanded.
     *//*

    private final Set<Integer> showingFriends = new HashSet<Integer>();

    */
/**
     * Column to control selection.
     *//*

    private Column<ContactInfo, Boolean> checkboxColumn;

    */
/**
     * Column to expand friends list.
     *//*

    private Column<ContactInfo, String> viewFriendsColumn;

    */
/**
     * Column displays first name.
     *//*

    private Column<ContactInfo, String> firstNameColumn;

    */
/**
     * Column displays last name.
     *//*

    private Column<ContactInfo, String> lastNameColumn;

    */
/**
     * Column displays age.
     *//*

    private Column<ContactInfo, Number> ageColumn;

    */
/**
     * Column displays category.
     *//*

    private Column<ContactInfo, String> categoryColumn;

    */
/**
     * Column displays address.
     *//*

    private Column<ContactInfo, String> addressColumn;

    */
/**
     * Initialize this example.
     *//*

    @Override
    public Widget onInitialize() {
        resources = GWT.create(Resources.class);
        resources.styles().ensureInjected();

        // Create a DataGrid.

    */
/*
     * Set a key provider that provides a unique key for each contact. If key is
     * used to identify contacts when fields (such as the name and address)
     * change.
     *//*

        dataGrid = new DataGrid<ContactInfo>(ContactDatabase.ContactInfo.KEY_PROVIDER);
        dataGrid.setWidth("100%");

    */
/*
     * Do not refresh the headers every time the data is updated. The footer
     * depends on the current data, so we do not disable auto refresh on the
     * footer.
     *//*

        dataGrid.setAutoHeaderRefreshDisabled(true);

        // Set the message to display when the table is empty.
        dataGrid.setEmptyTableWidget(new Label(constants.cwCustomDataGridEmpty()));

        // Attach a column sort handler to the ListDataProvider to sort the list.
        ListHandler<ContactInfo> sortHandler =
                new ListHandler<ContactInfo>(ContactDatabase.get().getDataProvider().getList());
        dataGrid.addColumnSortHandler(sortHandler);

        // Create a Pager to control the table.
        SimplePager.Resources pagerResources = GWT.create(SimplePager.Resources.class);
        pager = new SimplePager(TextLocation.CENTER, pagerResources, false, 0, true);
        pager.setDisplay(dataGrid);

        // Add a selection model so we can select cells.
        final SelectionModel<ContactInfo> selectionModel =
                new MultiSelectionModel<ContactInfo>(ContactDatabase.ContactInfo.KEY_PROVIDER);
        dataGrid.setSelectionModel(selectionModel, DefaultSelectionEventManager
                .<ContactInfo> createCheckboxManager());

        // Initialize the columns.
        initializeColumns(sortHandler);

        // Specify a custom table.
        dataGrid.setTableBuilder(new CustomTableBuilder());
        dataGrid.setHeaderBuilder(new CustomHeaderBuilder());
        dataGrid.setFooterBuilder(new CustomFooterBuilder());

        // Add the CellList to the adapter in the database.
        ContactDatabase.get().addDataDisplay(dataGrid);

        // Create the UiBinder.
        Binder uiBinder = GWT.create(Binder.class);
        return uiBinder.createAndBindUi(this);
    }

    */
/**
     * Defines the columns in the custom table. Maps the data in the ContactInfo
     * for each row into the appropriate column in the table, and defines handlers
     * for each column.
     *//*

    private void initializeColumns(ListHandler<ContactInfo> sortHandler) {
    */
/*
     * Checkbox column.
     *
     * This table will uses a checkbox column for selection. Alternatively, you
     * can call dataGrid.setSelectionEnabled(true) to enable mouse selection.
     *//*

        checkboxColumn = new Column<ContactInfo, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(ContactInfo object) {
                // Get the value from the selection model.
                return dataGrid.getSelectionModel().isSelected(object);
            }
        };
        dataGrid.setColumnWidth(0, 40, Unit.PX);

        // View friends.
        SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
            @Override
            public SafeHtml render(String object) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("(<a href=\"javascript:;\">").appendEscaped(object)
                        .appendHtmlConstant("</a>)");
                return sb.toSafeHtml();
            }
        };
        viewFriendsColumn = new Column<ContactInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(ContactInfo object) {
                if (showingFriends.contains(object.getId())) {
                    return "hide friends";
                } else {
                    return "show friends";
                }
            }
        };
        viewFriendsColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
            @Override
            public void update(int index, ContactInfo object, String value) {
                if (showingFriends.contains(object.getId())) {
                    showingFriends.remove(object.getId());
                } else {
                    showingFriends.add(object.getId());
                }

                // Redraw the modified row.
                dataGrid.redrawRow(index);
            }
        });
        dataGrid.setColumnWidth(1, 10, Unit.EM);

        // First name.
        firstNameColumn = new Column<ContactInfo, String>(new EditTextCell()) {
            @Override
            public String getValue(ContactInfo object) {
                return object.getFirstName();
            }
        };
        firstNameColumn.setSortable(true);
        sortHandler.setComparator(firstNameColumn, new Comparator<ContactInfo>() {
            @Override
            public int compare(ContactInfo o1, ContactInfo o2) {
                return o1.getFirstName().compareTo(o2.getFirstName());
            }
        });
        firstNameColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
            @Override
            public void update(int index, ContactInfo object, String value) {
                // Called when the user changes the value.
                object.setFirstName(value);
                ContactDatabase.get().refreshDisplays();
            }
        });
        dataGrid.setColumnWidth(2, 20, Unit.PCT);

        // Last name.
        lastNameColumn = new Column<ContactInfo, String>(new EditTextCell()) {
            @Override
            public String getValue(ContactInfo object) {
                return object.getLastName();
            }
        };
        lastNameColumn.setSortable(true);
        sortHandler.setComparator(lastNameColumn, new Comparator<ContactInfo>() {
            @Override
            public int compare(ContactInfo o1, ContactInfo o2) {
                return o1.getLastName().compareTo(o2.getLastName());
            }
        });
        lastNameColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
            @Override
            public void update(int index, ContactInfo object, String value) {
                // Called when the user changes the value.
                object.setLastName(value);
                ContactDatabase.get().refreshDisplays();
            }
        });
        dataGrid.setColumnWidth(3, 20, Unit.PCT);

        // Age.
        ageColumn = new Column<ContactInfo, Number>(new NumberCell()) {
            @Override
            public Number getValue(ContactInfo object) {
                return object.getAge();
            }
        };
        ageColumn.setSortable(true);
        sortHandler.setComparator(ageColumn, new Comparator<ContactInfo>() {
            @Override
            public int compare(ContactInfo o1, ContactInfo o2) {
                return o1.getAge() - o2.getAge();
            }
        });
        dataGrid.setColumnWidth(4, 7, Unit.EM);

        // Category.
        final Category[] categories = ContactDatabase.get().queryCategories();
        List<String> categoryNames = new ArrayList<String>();
        for (Category category : categories) {
            categoryNames.add(category.getDisplayName());
        }
        SelectionCell categoryCell = new SelectionCell(categoryNames);
        categoryColumn = new Column<ContactInfo, String>(categoryCell) {
            @Override
            public String getValue(ContactInfo object) {
                return object.getCategory().getDisplayName();
            }
        };
        categoryColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
            @Override
            public void update(int index, ContactInfo object, String value) {
                for (Category category : categories) {
                    if (category.getDisplayName().equals(value)) {
                        object.setCategory(category);
                    }
                }
                ContactDatabase.get().refreshDisplays();
            }
        });
        dataGrid.setColumnWidth(5, 130, Unit.PX);

        // Address.
        addressColumn = new Column<ContactInfo, String>(new TextCell()) {
            @Override
            public String getValue(ContactInfo object) {
                return object.getAddress();
            }
        };
        addressColumn.setSortable(true);
        sortHandler.setComparator(addressColumn, new Comparator<ContactInfo>() {
            @Override
            public int compare(ContactInfo o1, ContactInfo o2) {
                return o1.getAddress().compareTo(o2.getAddress());
            }
        });
        dataGrid.setColumnWidth(6, 60, Unit.PCT);
    }

*/
