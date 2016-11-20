package com.kodis.ui.component;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.widget.EditText;
import com.kodis.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderEditor extends EditText {
    private static final Pattern PATTERN_LINE = Pattern.compile(
            ".*\\n");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile(
            "\\b(\\d*[.]?\\d+)\\b");
    private static final Pattern PATTERN_PREPROCESSOR = Pattern.compile(
            "^[\t ]*(#define|#undef|#if|#ifdef|#ifndef|#else|#elif|#endif|" +
                    "#error|#pragma|#extension|#version|#line|#include)\\b",
            Pattern.MULTILINE);
    private static final Pattern PATTERN_KEYWORDS = Pattern.compile(
            "\\b(" +
                    "and|or|xor|for|do|while|foreach|as|return|die|exit|if|then|else|" +
                    "elseif|new|delete|try|throw|catch|finally|class|function|string|" +
                    "array|object|resource|var|bool|boolean|int|integer|float|double|" +
                    "real|string|array|global|const|static|public|private|protected|" +
                    "published|extends|switch|true|false|null|void|this|self|struct|" +
                    "char|signed|unsigned|short|long|True|False|a|address|app|applet|" +
                    "area|b|base|basefont|bgsound|big|blink|blockquote|body|br|button|" +
                    "caption|center|cite|code|col|colgroup|comment|dd|del|dfn|dir|div|" +
                    "dl|dt|em|embed|fieldset|font|form|frame|frameset|h1|h2|h3|h4|h5|h6|" +
                    "head|hr|html|htmlplus|hype|i|iframe|img|input|ins|del|isindex|kbd|" +
                    "label|legend|li|link|listing|map|marquee|menu|meta|multicol|nobr|" +
                    "noembed|noframes|noscript|ol|option|p|param|plaintext|pre|s|samp|" +
                    "script|select|small|sound|spacer|span|strike|strong|style|sub|sup|" +
                    "table|tbody|td|textarea|tfoot|th|thead|title|tr|tt|u|var|wbr|xmp" +
                    ")\\b");
    private static final Pattern PATTERN_BUILTINS = Pattern.compile(
            "\\b(radians|degrees|sin|cos|tan|asin|acos|atan|pow|" +
                    "exp|log|sqrt|inversesqrt|abs|sign|floor|ceil|fract|mod|" +
                    "min|max|length|Math|System|out|printf|print|println|" +
                    "console|Arrays|Array|vector|List|list|ArrayList|Map|HashMap|" +
                    "dict|java|util|lang|import|from|in|charset|lang|href|name|" +
                    "target|onclick|onmouseover|onmouseout|accesskey|code|codebase|" +
                    "width|height|align|vspace|hspace|border|name|archive|mayscript|" +
                    "alt|shape|coords|target|nohref|size|color|face|src|loop|bgcolor|" +
                    "background|text|vlink|alink|bgproperties|topmargin|leftmargin|" +
                    "marginheight|marginwidth|onload|onunload|onfocus|onblur|stylesrc|" +
                    "scroll|clear|type|value|valign|span|compact|pluginspage|pluginurl|" +
                    "hidden|autostart|playcount|volume|controls|controller|mastersound|" +
                    "starttime|endtime|point-size|weight|action|method|enctype|onsubmit|" +
                    "onreset|scrolling|noresize|frameborder|bordercolor|cols|rows|" +
                    "framespacing|border|noshade|longdesc|ismap|usemap|lowsrc|naturalsizeflag|" +
                    "nosave|dynsrc|controls|start|suppress|maxlength|checked|language|onchange|" +
                    "onkeypress|onkeyup|onkeydown|autocomplete|prompt|for|rel|rev|media|direction|" +
                    "behaviour|scrolldelay|scrollamount|http-equiv|content|gutter|defer|event|" +
                    "multiple|readonly|cellpadding|cellspacing|rules|bordercolorlight|" +
                    "bordercolordark|summary|colspan|rowspan|nowrap|halign|disabled|accesskey|" +
                    "tabindex|id)\\b");
    private static final Pattern PATTERN_COMMENTS = Pattern.compile(
            "/\\*(?:.|[\\n\\r])*?\\*/|//.*");
    private static final Pattern PATTERN_TRAILING_WHITE_SPACE = Pattern.compile(
            "[\\t ]+$",
            Pattern.MULTILINE);
    private final Handler updateHandler = new Handler();
    private OnTextChangedListener onTextChangedListener;
    private int updateDelay = 1000;
    private int errorLine = 0;
    private boolean dirty = false;
    private boolean modified = true;
    private int colorVariable;
    private int colorNumber;
    private int colorKeyword;
    private int colorBuiltin;
    private int colorComment;
    private final Runnable updateRunnable =
            new Runnable() {
                @Override
                public void run() {
                    Editable e = getText();

                    if (onTextChangedListener != null)
                        onTextChangedListener.onTextChanged(
                                e.toString());

                    highlightWithoutChange(e);
                }
            };
    private int tabWidthInCharacters = 0;
    private int tabWidth = 0;

    public ShaderEditor(Context context) {
        super(context);

        init(context);
    }

    public ShaderEditor(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    private static void clearSpans(Editable e) {
        // remove foreground color spans
        {
            ForegroundColorSpan spans[] = e.getSpans(
                    0,
                    e.length(),
                    ForegroundColorSpan.class);

            for (int n = spans.length; n-- > 0; )
                e.removeSpan(spans[n]);
        }

        // remove background color spans
        {
            BackgroundColorSpan spans[] = e.getSpans(
                    0,
                    e.length(),
                    BackgroundColorSpan.class);

            for (int n = spans.length; n-- > 0; )
                e.removeSpan(spans[n]);
        }
    }

    public void setOnTextChangedListener(OnTextChangedListener listener) {
        onTextChangedListener = listener;
    }

    public void setUpdateDelay(int ms) {
        updateDelay = ms;
    }

    public void setTabWidth(int characters) {
        if (tabWidthInCharacters == characters)
            return;

        tabWidthInCharacters = characters;
        tabWidth = Math.round(
                getPaint().measureText("m") *
                        characters);
    }

    public boolean hasErrorLine() {
        return errorLine > 0;
    }

    public void setErrorLine(int line) {
        errorLine = line;
    }

    public void updateHighlighting() {
        highlightWithoutChange(getText());
    }

    public boolean isModified() {
        return dirty;
    }

    public void setTextHighlighted(CharSequence text) {
        if (text == null)
            text = "";

        cancelUpdate();

        errorLine = 0;
        dirty = false;

        modified = false;
        setText(highlight(new SpannableStringBuilder(text)));
        modified = true;

        if (onTextChangedListener != null)
            onTextChangedListener.onTextChanged(text.toString());
    }

    public String getCleanText() {
        return PATTERN_TRAILING_WHITE_SPACE
                .matcher(getText())
                .replaceAll("");
    }

    public void insertTab() {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        getText().replace(
                Math.min(start, end),
                Math.max(start, end),
                "\t",
                0,
                1);
    }

    private void init(Context context) {
        setHorizontallyScrolling(true);

        setFilters(new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(
                            CharSequence source,
                            int start,
                            int end,
                            Spanned dest,
                            int dstart,
                            int dend) {
                        if (modified &&
                                end - start == 1 &&
                                start < source.length() &&
                                dstart < dest.length()) {
                            char c = source.charAt(start);

                            if (c == '\n')
                                return autoIndent(
                                        source,
                                        dest,
                                        dstart,
                                        dend);
                        }

                        return source;
                    }
                }});

        addTextChangedListener(
                new TextWatcher() {
                    private int start = 0;
                    private int count = 0;

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        this.start = start;
                        this.count = count;
                    }

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable e) {
                        cancelUpdate();
                        convertTabs(e, start, count);

                        if (!modified)
                            return;

                        dirty = true;
                        updateHandler.postDelayed(
                                updateRunnable,
                                updateDelay);
                    }
                });

        setSyntaxColors(context);
        /*setUpdateDelay(
            ShaderEditorApplication
				.preferences
				.getUpdateDelay() );
		setTabWidth(
			ShaderEditorApplication
				.preferences
				.getTabWidth() );*/
    }

    private void setSyntaxColors(Context context) {
        colorVariable = ContextCompat.getColor(context, R.color.syntax_variable);
        colorNumber = ContextCompat.getColor(
                context,
                R.color.syntax_number);
        colorKeyword = ContextCompat.getColor(
                context,
                R.color.syntax_keyword);
        colorBuiltin = ContextCompat.getColor(
                context,
                R.color.syntax_builtin);
        colorComment = ContextCompat.getColor(
                context,
                R.color.syntax_comment);

    }

    private void cancelUpdate() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void highlightWithoutChange(Editable e) {
        modified = false;
        highlight(e);
        modified = true;
    }

    private Editable highlight(Editable e) {
        try {
            // don't use e.clearSpans() because it will
            // remove too much
            clearSpans(e);

            if (e.length() == 0)
                return e;

            for (Matcher m = PATTERN_NUMBERS.matcher(e); m.find(); )
                e.setSpan(new ForegroundColorSpan(colorNumber), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            for (Matcher m = PATTERN_PREPROCESSOR.matcher(e); m.find(); )
                e.setSpan(new ForegroundColorSpan(colorBuiltin), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            for (Matcher m = PATTERN_KEYWORDS.matcher(e); m.find(); )
                e.setSpan(new ForegroundColorSpan(colorKeyword), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            for (Matcher m = PATTERN_BUILTINS.matcher(e); m.find(); )
                e.setSpan(new ForegroundColorSpan(colorBuiltin), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            for(Matcher m = Pattern.compile("\\$\\w+").matcher(e); m.find(); ) {
                e.setSpan(new ForegroundColorSpan(colorVariable), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for(Matcher m = Pattern.compile("\\\"(.*?)\\\"|\\\'(.*?)\\\'").matcher(e); m.find(); ) {
                ForegroundColorSpan spans[] = e.getSpans(m.start(), m.end(), ForegroundColorSpan.class);
                for(ForegroundColorSpan span : spans)
                    e.removeSpan(span);
                e.setSpan(new ForegroundColorSpan(Color.parseColor("#81C784")), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (Matcher m = PATTERN_COMMENTS.matcher(e); m.find(); ) {
                ForegroundColorSpan spans[] = e.getSpans(m.start(), m.end(), ForegroundColorSpan.class);
                for(ForegroundColorSpan span : spans)
                    e.removeSpan(span);
                e.setSpan(new ForegroundColorSpan(colorComment), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (IllegalStateException ex) {
            // raised by Matcher.start()/.end() when
            // no successful match has been made what
            // shouldn't ever happen because of find()
        }

        return e;
    }

    private CharSequence autoIndent(
            CharSequence source,
            Spanned dest,
            int dstart,
            int dend) {
        String indent = "";
        int istart = dstart - 1;

        // find start of this line
        boolean dataBefore = false;
        int pt = 0;

        for (; istart > -1; --istart) {
            char c = dest.charAt(istart);

            if (c == '\n')
                break;

            if (c != ' ' &&
                    c != '\t') {
                if (!dataBefore) {
                    // indent always after those characters
                    if (c == '{' ||
                            c == '+' ||
                            c == '-' ||
                            c == '*' ||
                            c == '/' ||
                            c == '%' ||
                            c == '^' ||
                            c == '=')
                        pt--;

                    dataBefore = true;
                }

                // parenthesis counter
                if (c == '(')
                    --pt;
                else if (c == ')')
                    ++pt;
            }
        }

        // copy indent of this line into the next
        if (istart > -1) {
            char charAtCursor = dest.charAt(dstart);
            int iend;

            for (iend = ++istart;
                 iend < dend;
                 ++iend) {
                char c = dest.charAt(iend);

                // auto expand comments
                if (charAtCursor != '\n' &&
                        c == '/' &&
                        iend + 1 < dend &&
                        dest.charAt(iend) == c) {
                    iend += 2;
                    break;
                }

                if (c != ' ' &&
                        c != '\t')
                    break;
            }

            indent += dest.subSequence(istart, iend);
        }

        // add new indent
        if (pt < 0)
            indent += "\t";

        // append white space of previous line and new indent
        return source + indent;
    }

    private void convertTabs(Editable e, int start, int count) {
        if (tabWidth < 1)
            return;

        String s = e.toString();

        for (int stop = start + count;
             (start = s.indexOf("\t", start)) > -1 && start < stop;
             ++start)
            e.setSpan(
                    new TabWidthSpan(),
                    start,
                    start + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private class TabWidthSpan extends ReplacementSpan {
        @Override
        public int getSize(
                Paint paint,
                CharSequence text,
                int start,
                int end,
                Paint.FontMetricsInt fm) {
            return tabWidth;
        }

        @Override
        public void draw(
                Canvas canvas,
                CharSequence text,
                int start,
                int end,
                float x,
                int top,
                int y,
                int bottom,
                Paint paint) {
        }
    }
}
