//
// C++ Interface: settings
//
// Description:
//
//
// Author: Vadim Lopatin <vadim.lopatin@coolreader.org>, (C) 2008
//
// Copyright: See COPYING file that comes with this distribution
//
//

#ifndef CR3_SETTINGS_H_INCLUDED
#define CR3_SETTINGS_H_INCLUDED

#include <crgui.h>
#include "fsmenu.h"

#define MENU_FONT_SIZE 28
#define MENU_FONT_FACE_SIZE 36
#define VALUE_FONT_SIZE 24

typedef struct {
    const char * translate_default;
    const char * value;
} item_def_t;

#define SETTINGS_MENU_COMMANDS_START 300
enum MainMenuItems_t {
    mm_Settings = SETTINGS_MENU_COMMANDS_START,
    mm_FontFace,
    mm_FontSize,
    mm_FontAntiAliasing,
    mm_InterlineSpace,
    mm_Orientation,
    mm_EmbeddedStyles,
    mm_Inverse,
    mm_StatusLine,
    mm_BookmarkIcons,
    mm_Footnotes,
    mm_SetTime,
    mm_ShowTime,
    mm_Kerning,
    mm_LandscapePages,
    mm_PreformattedText,
    mm_PageMargins,
    mm_PageMarginTop,
    mm_PageMarginLeft,
    mm_PageMarginRight,
    mm_PageMarginBottom,
    mm_Hyphenation,
    mm_Controls,
    mm_Embolden,
};


#define DECL_DEF_CR_FONT_SIZES static int cr_font_sizes[] = \
 { 20, 22, 24, 26, 28, 32, 36, 42, 48, 56 }
 //{ 18, 20, 22, 24, 26, 28, 32, 36, 42, 48 }
//    2    2   2   4   4   4   6   6   8
//{ 18, 20, 22, 24, 26, 28, 32, 38, 42, 48 }
//    2    2   2   2   2   4   6   4   6


class CRSettingsMenu : public CRFullScreenMenu
{
    protected:
        CRPropRef props;
        CRGUIAcceleratorTableRef _menuAccelerators;
        void addMenuItems( CRMenu * menu, item_def_t values[] );
    public:
        CRMenu * createFontSizeMenu( CRMenu * mainMenu, CRPropRef props );
#if CR_INTERNAL_PAGE_ORIENTATION==1
        CRMenu * createOrientationMenu( CRMenu * mainMenu, CRPropRef props );
#endif
        CRSettingsMenu( CRGUIWindowManager * wm, CRPropRef props, int id, LVFontRef font, CRGUIAcceleratorTableRef menuAccelerators, lvRect & rc );
        virtual bool onCommand( int command, int params );
};


#endif //CR3_SETTINGS_H_INCLUDED
