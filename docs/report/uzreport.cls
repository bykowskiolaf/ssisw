\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{uzreport}[2019/11/25 UZ Report Template]

\LoadClassWithOptions{report}
\RequirePackage{subcaption}
\RequirePackage{cite}
\RequirePackage{multicol}
\RequirePackage{enumitem}
\RequirePackage[utf8]{inputenc}     % Accept different input encodings
\RequirePackage[T1]{fontenc}        % Standard package for selecting font encodings
\RequirePackage{amsmath}    % AMS mathematical facilities for LaTeX
\RequirePackage{polski}
\RequirePackage{amssymb}    % AMS symbols for LaTeX
\RequirePackage{amsthm}     % Typesetting theorems (AMS style)
\RequirePackage{amsfonts}   % TeX fonts from the American Mathematical Society
\RequirePackage{amsbsy}     % Produce bold math symbols (AMS-LaTeX)

\RequirePackage{color}                  % Colour control for LaTeX documents
\RequirePackage[dvipsnames]{xcolor}     % Driver-independent color extensions for LaTeX and pdfLaTeX
\RequirePackage{tikz}                   % TeX packages for creating graphics programmatically
\RequirePackage{pgfplots}

\RequirePackage{graphicx}   % Enhanced support for graphics
\RequirePackage{epstopdf}   % Convert EPS to PDF using Ghostscript

\RequirePackage{multirow}   % Create tabular cells spanning multiple rows
\RequirePackage{longtable}  % Allow tables to flow over page boundaries
\RequirePackage{array}      % Extending the array and tabular environments

\RequirePackage{indentfirst}    % Indent first paragraph after section header
\RequirePackage{hyperref}       % Extensive support for hypertext in LaTeX
\RequirePackage{datetime}   %for date printing and manipulation
\RequirePackage{siunitx}    % A comprehensive (SI) units package
\RequirePackage{listingsutf8}

\RequirePackage{lmodern} 
\RequirePackage{float}
\RequirePackage{geometry}      % Flexible and complete interface to document dimensions
\geometry{
	paper=a4paper,         % Change to letterpaper for US letter
	outer=2cm,           % Left margin
	inner=2cm,           % Right margin
	top=2.5cm,             % Top margin
	bottom=2.5cm,          % Bottom margin
	% showframe,            % Uncomment to show how the type block is set on the page
}


\definecolor{uzgreen}{rgb}{0,0.6,0}
\definecolor{uzgray}{rgb}{0.5,0.5,0.5}
\definecolor{uzmauve}{rgb}{0.58,0,0.82}
\definecolor{uzlogoblue}{rgb}{0.25,0.47,0.52}

\lstdefinestyle{usreportlst}{ %
  backgroundcolor=\color{white},   % choose the background color; you must add \usepackage{color} or \usepackage{xcolor}; should come as last argument
  basicstyle=\linespread{1}\small, % the size of the fonts that are used for the code
  breakatwhitespace=false,         % sets if automatic breaks should only happen at whitespace
  breaklines=true,                 % sets automatic line breaking
  captionpos=b,                    % sets the caption-position to bottom
  commentstyle=\color{uzgreen},    % comment style
  deletekeywords={...},            % if you want to delete keywords from the given language
  escapeinside={\%*}{*)},          % if you want to add LaTeX within your code
  frame=single,	                 % adds a frame around the code
  keepspaces=true,                 % keeps spaces in text, useful for keeping indentation of code (possibly needs columns=flexible)
  keywordstyle=\color{blue},       % keyword style
  % language=Matlab,                 % the language of the code
  numbers=left,                    % where to put the line-numbers; possible values are (none, left, right)
  numbersep=7pt,                   % how far the line-numbers are from the code
  numberstyle=\tiny\color{uzgray}, % the style that is used for the line-numbers
  rulecolor=\color{uzgray},        % if not set, the frame-color may be changed on line-breaks within not-black text (e.g. comments (green here))
  showspaces=false,                % show spaces everywhere adding particular underscores; it overrides 'showstringspaces'
  showstringspaces=false,          % underline spaces within strings only
  showtabs=false,                  % show tabs within strings adding particular underscores
  stepnumber=1,                    % the step between two line-numbers. If it's 1, each line will be numbered
  stringstyle=\color{uzmauve},     % string literal style
  tabsize=2,	                   % sets default tabsize to 2 spaces
  numberbychapter=true,
}

\newcommand{\matlabCode}[3]{\lstinputlisting[caption=#2,style=usreportlst,language=Matlab,label=#3]{#1}}
\newcommand{\pythonCode}[3]{\lstinputlisting[caption=#2,style=usreportlst,language=Python,label=#3]{#1}}
\newcommand{\anyCode}[4]{\lstinputlisting[caption=#3,style=usreportlst,language=#1,label=#4]{#2}}
 
\theoremstyle{definition}
\newtheorem{definition}{Definition}[chapter]
\newtheorem{assum}{Assumption}[chapter]
\newtheorem{theorem}{Theorem}[chapter]
\newtheorem{lemma}{Lemma}[chapter]
 
\theoremstyle{remark}
\newtheorem{remark}{Remark}[chapter]

% ----------------------------------------------------------------
% date formats
% ----------------------------------------------------------------
\newdateformat{monthyeardate}{%
  \monthname[\THEMONTH] \THEYEAR
}
\newdateformat{fulldate}{%
  {\twodigit{\THEDAY}.\twodigit{\THEMONTH}.\THEYEAR}
}
% ----------------------------------------------------------------
% color control
% ----------------------------------------------------------------
\newcommand{\currentColor}{\color{Blue}}
\DeclareOption{green}{
  \renewcommand{\currentColor}{\color{Green}}
}
\DeclareOption{black}{
  \renewcommand{\currentColor}{\color{Black}}
}
\DeclareOption{uselistingsutf8}{
  %conditional for listingsutf8 / copy for tables
}

% ----------------------------------------------------------------
% process class options
% ----------------------------------------------------------------
\ProcessOptions\relax

% ----------------------------------------------------------------
% enumarate and itemize customization
% ----------------------------------------------------------------
\renewcommand{\labelitemi}{\raisebox{0.25ex}{\tiny{$\blacksquare$}}}

\renewcommand*\thesection{\arabic{section}}
\setcounter{section}{0}

\renewcommand{\and}{\\}
\makeatletter
\newcommand{\lab}[1]{\def\@lab{#1}}
\newcommand{\labnumber}[1]{\def\@labnumber{#1}}
\newcommand{\group}[1]{\def\@group{#1}}
\newcommand{\supervisor}[1]{\def\@supervisor{#1}}
\newcommand{\class}[1]{\def\@class{#1}}
\newcommand{\type}[1]{\def\@type{#1}}
\renewcommand*{\maketitle}{%
\begin{titlepage}
\begin{center}
    \includegraphics[width=.55\textwidth]{assets/logo}
\end{center}
\vspace{7em}
\begin{center}
{\Huge Sprawozdanie z
 \if\@type l
laboratorium
 \else
projektu
 \fi
 \\ \LARGE \@class\\[4em]}
{\LARGE \@labnumber: \@lab\\[3em]}
{\LARGE\@date\\[3em]}
{\color{uzlogoblue}\hrule}
\end{center}
\vspace{3em}
\begin{minipage}[t]{0.64\textwidth}
 {Prowadzący: \@supervisor}
\end{minipage}
\begin{minipage}[t]{0.34\textwidth}
{Grupa: \@group \\[1em]}
{\@author}
\end{minipage}
\end{titlepage}
}
\makeatother

\linespread{1.25}
\endinput
