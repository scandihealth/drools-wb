/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.rule.client.editor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.drools.workbench.models.datamodel.oracle.DataType;
import org.drools.workbench.models.datamodel.rule.RuleAttribute;
import org.drools.workbench.models.datamodel.rule.RuleMetadata;
import org.drools.workbench.models.datamodel.rule.RuleModel;
import org.drools.workbench.screens.guided.rule.client.resources.GuidedRuleEditorResources;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.kie.workbench.common.widgets.client.resources.ItemImages;
import org.kie.workbench.common.widgets.client.widget.TextBoxFactory;
import org.uberfire.ext.widgets.common.client.common.DatePicker;
import org.uberfire.ext.widgets.common.client.common.DirtyableHorizontalPane;
import org.uberfire.ext.widgets.common.client.common.FormStyleLayout;
import org.uberfire.ext.widgets.common.client.common.InfoPopup;
import org.uberfire.ext.widgets.common.client.common.SmallLabel;

/**
 * Displays a list of rule options (attributes).
 * <p/>
 * Added support for metadata - Michael Rhoden 10/17/08
 */
public class RuleAttributeWidget extends Composite {

    private static final String DATE_FORMAT = ApplicationPreferences.getDroolsDateFormat();
    private static final DateTimeFormat DATE_FORMATTER = DateTimeFormat.getFormat( DATE_FORMAT );

    /**
     * These are the names of all of the rule attributes for this widget
     */
    public static final String SALIENCE_ATTR = "salience";         // needs to be public
    public static final String ENABLED_ATTR = "enabled";
    public static final String DATE_EFFECTIVE_ATTR = "date-effective";
    public static final String DATE_EXPIRES_ATTR = "date-expires";
    public static final String NO_LOOP_ATTR = "no-loop";
    public static final String AGENDA_GROUP_ATTR = "agenda-group";
    public static final String ACTIVATION_GROUP_ATTR = "activation-group";
    public static final String DURATION_ATTR = "duration";
    public static final String TIMER_ATTR = "timer";
    public static final String CALENDARS_ATTR = "calendars";
    public static final String AUTO_FOCUS_ATTR = "auto-focus";
    public static final String LOCK_ON_ACTIVE_ATTR = "lock-on-active";
    public static final String RULEFLOW_GROUP_ATTR = "ruleflow-group";
    public static final String DIALECT_ATTR = "dialect";
    public static final String LOCK_LHS = "freeze_conditions";
    public static final String LOCK_RHS = "freeze_actions";

    public static final String DEFAULT_DIALECT = "mvel";
    public static final String[] DIALECTS = {"java", "mvel"};

    /**
     * If the rule attribute is represented visually by a checkbox, these are
     * the values that will be stored in the model when checked/unchecked
     */
    private static final String TRUE_VALUE = "true";
    private static final String FALSE_VALUE = "false";

    private RuleModel model;
    private RuleModeller parent;

    public RuleAttributeWidget( final RuleModeller parent,
                                final RuleModel model,
                                final boolean isReadOnly ) {
        this.parent = parent;
        this.model = model;
        FormStyleLayout layout = new FormStyleLayout();
        //Adding metadata here, seems redundant to add a new widget for metadata. Model does handle meta data separate.
        RuleMetadata[] meta = model.metadataList;

        //Dont display "system managed" LPR metadata in the widget
        List<RuleMetadata> lprMeta = new ArrayList<RuleMetadata>();
        for ( RuleMetadata ruleMetadata : meta ) {
            if(ruleMetadata.getAttributeName().startsWith(  LprMetadataConsts.LPRMETA  )) {
                lprMeta.add( ruleMetadata );
            }
        }
        if ( meta.length > 0 && meta.length > lprMeta.size() ) {
            HorizontalPanel hp = new HorizontalPanel();
            hp.add( new SmallLabel( GuidedRuleEditorResources.CONSTANTS.Metadata2() ) );
            layout.addRow( hp );
        }
        for ( int i = 0; i < meta.length; i++ ) {
            RuleMetadata rmd = meta[i];
            if ( !rmd.getAttributeName().startsWith( LprMetadataConsts.LPRMETA ) ) {
                layout.addAttribute( rmd.getAttributeName(),
                        getEditorWidget( rmd,
                                i,
                                isReadOnly ) );
            }
        }
        RuleAttribute[] attrs = model.attributes;
        if ( attrs.length > 0 ) {
            HorizontalPanel hp = new HorizontalPanel();
            hp.add( new SmallLabel( GuidedRuleEditorResources.CONSTANTS.Attributes1() ) );
            layout.addRow( hp );
        }
        for ( int i = 0; i < attrs.length; i++ ) {
            RuleAttribute at = attrs[i];
            layout.addAttribute( at.getAttributeName(),
                    getEditorWidget( at,
                            i,
                            isReadOnly ) );
        }

        initWidget( layout );
    }

    /**
     * Return a listbox of choices for rule attributes.
     * @return
     */
    public static ListBox getAttributeList() {
        ListBox list = new ListBox();
        list.addItem( GuidedRuleEditorResources.CONSTANTS.Choose() );

        list.addItem( SALIENCE_ATTR );
        list.addItem( ENABLED_ATTR );
        list.addItem( DATE_EFFECTIVE_ATTR );
        list.addItem( DATE_EXPIRES_ATTR );
        list.addItem( NO_LOOP_ATTR );
        list.addItem( AGENDA_GROUP_ATTR );
        list.addItem( ACTIVATION_GROUP_ATTR );
        list.addItem( DURATION_ATTR );
        list.addItem( TIMER_ATTR );
        list.addItem( CALENDARS_ATTR );
        list.addItem( AUTO_FOCUS_ATTR );
        list.addItem( LOCK_ON_ACTIVE_ATTR );
        list.addItem( RULEFLOW_GROUP_ATTR );
        list.addItem( DIALECT_ATTR );

        return list;
    }

    private Widget getEditorWidget( final RuleAttribute at,
                                    final int idx,
                                    final boolean isReadOnly ) {
        Widget editor = null;

        final String attributeName = at.getAttributeName();
        if ( attributeName.equals( RULEFLOW_GROUP_ATTR )
                || attributeName.equals( AGENDA_GROUP_ATTR )
                || attributeName.equals( ACTIVATION_GROUP_ATTR )
                || attributeName.equals( TIMER_ATTR )
                || attributeName.equals( CALENDARS_ATTR ) ) {
            final TextBox tb = TextBoxFactory.getTextBox( DataType.TYPE_STRING );
            tb.setEnabled( !isReadOnly );
            if ( !isReadOnly ) {
                tb.addValueChangeHandler( new ValueChangeHandler<String>() {

                    public void onValueChange( ValueChangeEvent<String> event ) {
                        at.setValue( tb.getValue() );
                    }

                } );
            }
            tb.setValue( at.getValue() );
            editor = tb;

        } else if ( attributeName.equals( SALIENCE_ATTR ) ) {
            final TextBox tb = TextBoxFactory.getTextBox( DataType.TYPE_NUMERIC_INTEGER );
            tb.setEnabled( !isReadOnly );
            if ( !isReadOnly ) {
                tb.addValueChangeHandler( new ValueChangeHandler<String>() {

                    public void onValueChange( ValueChangeEvent<String> event ) {
                        at.setValue( tb.getValue() );
                    }

                } );
            }
            tb.setValue( at.getValue() );
            editor = tb;

        } else if ( attributeName.equals( DURATION_ATTR ) ) {
            final TextBox tb = TextBoxFactory.getTextBox( DataType.TYPE_NUMERIC_LONG );
            tb.setEnabled( !isReadOnly );
            if ( !isReadOnly ) {
                tb.addValueChangeHandler( new ValueChangeHandler<String>() {

                    public void onValueChange( ValueChangeEvent<String> event ) {
                        at.setValue( tb.getValue() );
                    }

                } );
            }
            tb.setValue( at.getValue() );
            editor = tb;

        } else if ( attributeName.equals( NO_LOOP_ATTR )
                || attributeName.equals( LOCK_ON_ACTIVE_ATTR )
                || attributeName.equals( AUTO_FOCUS_ATTR )
                || attributeName.equals( ENABLED_ATTR ) ) {
            editor = checkBoxEditor( at,
                    isReadOnly );

        } else if ( attributeName.equals( DATE_EFFECTIVE_ATTR )
                || attributeName.equals( DATE_EXPIRES_ATTR ) ) {
            if ( isReadOnly ) {
                final TextBox tb = TextBoxFactory.getTextBox( DataType.TYPE_STRING );
                tb.setValue( at.getValue() );
                tb.setEnabled( false );
            } else {
                final DatePicker datePicker = new DatePicker( false );

                // Wire up update handler
                datePicker.addValueChangeHandler( new ValueChangeHandler<Date>() {
                    @Override
                    public void onValueChange( final ValueChangeEvent<Date> event ) {
                        final Date date = datePicker.getValue();
                        final String sDate = (date == null ? null : DATE_FORMATTER.format( datePicker.getValue() ));
                        at.setValue( sDate );
                    }
                } );

                datePicker.setFormat( DATE_FORMAT );
                datePicker.setValue( assertDateValue( at ) );

                editor = datePicker;
            }
        } else if ( attributeName.equals( DIALECT_ATTR ) ) {
            final ListBox lb = new ListBox();
            lb.addItem( DIALECTS[0] );
            lb.addItem( DIALECTS[1] );
            lb.setEnabled( !isReadOnly );
            if ( !isReadOnly ) {
                lb.addChangeHandler( new ChangeHandler() {
                    @Override
                    public void onChange( ChangeEvent event ) {
                        final int selectedIndex = lb.getSelectedIndex();
                        if ( selectedIndex < 0 ) {
                            return;
                        }
                        at.setValue( lb.getValue( selectedIndex ) );
                    }
                } );
            }
            if ( at.getValue() == null || at.getValue().isEmpty() ) {
                lb.setSelectedIndex( 1 );
                at.setValue( DIALECTS[1] );
            } else if ( at.getValue().equals( DIALECTS[0] ) ) {
                lb.setSelectedIndex( 0 );
            } else if ( at.getValue().equals( DIALECTS[1] ) ) {
                lb.setSelectedIndex( 1 );
            } else {
                lb.setSelectedIndex( 1 );
                at.setValue( DIALECTS[1] );
            }
            editor = lb;
        }

        DirtyableHorizontalPane horiz = new DirtyableHorizontalPane();
        if ( editor != null ) {
            horiz.add( editor );
            if ( !isReadOnly ) {
                horiz.add( getRemoveIcon( idx ) );
            }
        }

        return horiz;
    }

    private Date assertDateValue( final RuleAttribute at ) {
        if ( at.getValue() == null ) {
            return null;
        }

        try {
            return DATE_FORMATTER.parse( at.getValue() );

        } catch ( IllegalArgumentException iae ) {
            return null;
        }
    }

    private Widget getEditorWidget( final RuleMetadata rm,
                                    final int idx,
                                    final boolean isReadOnly ) {
        IsWidget editor;

        if ( rm.getAttributeName().equals( LOCK_LHS ) || rm.getAttributeName().equals( LOCK_RHS ) ) {
            editor = new InfoPopup( GuidedRuleEditorResources.CONSTANTS.FrozenAreas(),
                    GuidedRuleEditorResources.CONSTANTS.FrozenExplanation() );
        } else {
            editor = textBoxEditor( rm,
                    isReadOnly );
        }

        DirtyableHorizontalPane horiz = new DirtyableHorizontalPane();
        horiz.add( editor );
        if ( !isReadOnly ) {
            horiz.add( getRemoveMetaIcon( idx ) );
        }

        return horiz;
    }

    private Widget checkBoxEditor( final RuleAttribute at,
                                   final boolean isReadOnly ) {
        final CheckBox box = new CheckBox();
        box.setEnabled( !isReadOnly );
        if ( at.getValue() == null || at.getValue().isEmpty() ) {
            box.setValue( false );
            at.setValue( FALSE_VALUE );
        } else {
            box.setValue( (at.getValue().equals( TRUE_VALUE )) );
        }

        box.addClickHandler( new ClickHandler() {
            public void onClick( ClickEvent event ) {
                at.setValue( (box.getValue()) ? TRUE_VALUE : FALSE_VALUE );
            }
        } );
        return box;
    }

    private TextBox textBoxEditor( final RuleMetadata rm,
                                   final boolean isReadOnly ) {
        final TextBox box = new TextBox();
        box.setEnabled( !isReadOnly );
        (( InputElement ) box.getElement().cast()).setSize( (rm.getValue().length() < 3) ? 3 : rm.getValue().length() );
        box.setText( rm.getValue() );
        box.addChangeHandler( new ChangeHandler() {
            public void onChange( ChangeEvent event ) {
                rm.setValue( box.getText() );
            }
        } );

        box.addKeyUpHandler( new KeyUpHandler() {
            public void onKeyUp( KeyUpEvent event ) {
                (( InputElement ) box.getElement().cast()).setSize( box.getText().length() );
            }
        } );
        return box;
    }

    private Image getRemoveIcon( final int idx ) {
        Image remove = new Image( ItemImages.INSTANCE.deleteItemSmall() );
        remove.addClickHandler( new ClickHandler() {
            public void onClick( ClickEvent event ) {
                if ( Window.confirm( GuidedRuleEditorResources.CONSTANTS.RemoveThisRuleOption() ) ) {
                    model.removeAttribute( idx );
                    parent.refreshWidget();
                }
            }
        } );
        return remove;
    }

    private Image getRemoveMetaIcon( final int idx ) {
        Image remove = new Image( ItemImages.INSTANCE.deleteItemSmall() );
        remove.addClickHandler( new ClickHandler() {
            public void onClick( ClickEvent event ) {
                if ( Window.confirm( GuidedRuleEditorResources.CONSTANTS.RemoveThisRuleOption() ) ) {
                    model.removeMetadata( idx );
                    parent.refreshWidget();
                }
            }
        } );
        return remove;
    }
}
