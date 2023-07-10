package ru.skillbranch.skillarticles.ui

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.databinding.ActivityRootBinding
import ru.skillbranch.skillarticles.databinding.LayoutBottombarBinding
import ru.skillbranch.skillarticles.databinding.LayoutSubmenuBinding
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.Notify
import ru.skillbranch.skillarticles.viewmodels.ViewModelFactory


class RootActivity : AppCompatActivity() {

    private lateinit var viewModel: ArticleViewModel
    private lateinit var mBinding: ActivityRootBinding
    private lateinit var bbBinding: LayoutBottombarBinding
    private lateinit var smBinding: LayoutSubmenuBinding

    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityRootBinding.inflate(layoutInflater)
        bbBinding = LayoutBottombarBinding.bind(mBinding.bottombar)
        smBinding = LayoutSubmenuBinding.bind(mBinding.submenu)

        setContentView(mBinding.root)
        setupBottombar()
        setupSubmenu()

        val vmFactory = ViewModelFactory("0")
        viewModel = ViewModelProviders.of(this, vmFactory)[ArticleViewModel::class.java]
        viewModel.observeState(this) {
            if (!it.isSearch)
                setupToolbar()
            renderUi(it)
        }

        viewModel.observeNotifications(this) {
            renderNotification(it)
        }
    }

    override fun onResume() {
        super.onResume()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val menuItem: MenuItem = menu.findItem(R.id.action_search)
        searchView = menuItem.actionView as SearchView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));
        searchView.queryHint = "Search"

        searchView.isIconfiedByDefault

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                viewModel.handleIsSearch(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.handleIsSearch(false)
                return true
            }
        })

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun renderUi(data: ArticleState) {
        // bind submenu state
        bbBinding.btnSettings.isChecked = data.isShowMenu
        if (data.isShowMenu) mBinding.submenu.open() else mBinding.submenu.close()

        // bind article person data
        bbBinding.btnLike.isChecked = data.isLike
        bbBinding.btnBookmark.isChecked = data.isBookmark

        // bind submenu views
        smBinding.switchMode.isChecked = data.isDarkMode
        delegate.localNightMode =
            if (data.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (data.isBigText) {
            mBinding.tvTextContent.textSize = 18f
            smBinding.btnTextUp.isChecked = true
            smBinding.btnTextDown.isChecked = false
        } else {
            mBinding.tvTextContent.textSize = 14f
            smBinding.btnTextUp.isChecked = false
            smBinding.btnTextDown.isChecked = true
        }

        // bind content
        mBinding.tvTextContent.text =
            if (data.isLoadingContent) "loading" else data.content.first() as String

        // bind toolbar
        mBinding.toolbar.title = data.title ?: "Skill Articles"
        mBinding.toolbar.subtitle = data.category ?: "loading..."
        if (data.categoryIcon != null) mBinding.toolbar.logo = getDrawable(data.categoryIcon as Int)
    }

    private fun renderNotification(notify: Notify) {
        val snackbar =
            Snackbar.make(
                findViewById<CoordinatorLayout>(R.id.coordinator_container),
                notify.message,
                Snackbar.LENGTH_LONG
            )
                .setAnchorView(mBinding.bottombar)

        when (notify) {
            is Notify.TextMessage -> { /* nothing */
            }
            is Notify.ActionMessage -> {
                snackbar.setActionTextColor(getColor(R.color.color_accent_dark))
                snackbar.setAction(notify.actionLabel) {
                    notify.actionHandler?.invoke()
                }
            }
            is Notify.ErrorMessage -> {
                with(snackbar) {
                    setBackgroundTint(getColor(com.google.android.material.R.color.design_default_color_error))
                    setTextColor(getColor(android.R.color.white))
                    setActionTextColor(getColor(android.R.color.white))
                    setAction(notify.errLabel) {
                        notify.errHandler?.invoke()
                    }
                }
            }
        }

        snackbar.show()
    }

    private fun setupToolbar() {
        setSupportActionBar(mBinding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = mBinding.toolbar.children.find { it is AppCompatImageView } as? ImageView
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
        //check toolbar imports
        (logo?.layoutParams as? Toolbar.LayoutParams)?.let {
            it.width = dpToIntPx(40)
            it.height = dpToIntPx(40)
            it.marginEnd = dpToIntPx(16)
            logo.layoutParams = it
        }
    }

    private fun setupBottombar() {

        bbBinding.btnLike.setOnClickListener { viewModel.handleLike() }
        bbBinding.btnBookmark.setOnClickListener { viewModel.handleBookmark() }
        bbBinding.btnShare.setOnClickListener { viewModel.handleShare() }
        bbBinding.btnSettings.setOnClickListener { viewModel.handleToggleMenu() }
    }

    private fun setupSubmenu() {
        smBinding.btnTextUp.setOnClickListener { viewModel.handleUpText() }
        smBinding.btnTextDown.setOnClickListener { viewModel.handleDownText() }
        smBinding.switchMode.setOnClickListener { viewModel.handleNightMode() }
    }

}