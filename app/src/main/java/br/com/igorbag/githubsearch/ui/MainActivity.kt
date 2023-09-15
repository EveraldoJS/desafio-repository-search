package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService
    private var userlogin:String =""
    private val Tag ="github"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        showUserName()
        setupListeners()
        setupRetrofit()
        getAllReposByUserName()
    }

      // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    fun setupView() {
        //Recuperar os Id's da tela para a Activity com o findViewById
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories= findViewById(R.id.rv_lista_repositories)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        //colocar a acao de click do botao confirmar
        btnConfirmar.setOnClickListener {
        saveUserLocal()
        getAllReposByUserName()
    }
    }

    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal() {
        //Persistir o usuario preenchido na editText com a SharedPref no listener do botao salvar
        if(nomeUsuario.text != null) {
            userlogin = nomeUsuario.text.toString()
            saveSharedPref(userlogin)
            Log.i(Tag,"$userlogin cadastrado com sucesso!")
        }else{
            Log.i(Tag,"Preencha usuário correto.")
        }
    }

    private fun showUserName() {
        //depois de persistir o usuario exibir sempre as informacoes no EditText  se a sharedpref possuir algum valor, exibir no proprio editText o valor salvo
        nomeUsuario.setText(getSharedPref())
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    fun setupRetrofit() {
        /*
          Realizar a Configuracao base do retrofit
           Documentacao oficial do retrofit - https://square.github.io/retrofit/
           URL_BASE da API do  GitHub= https://api.github.com/
           lembre-se de utilizar o GsonConverterFactory mostrado no curso
        */
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = retrofit.create(GitHubService::class.java)

        Log.i(Tag,"Retrofit configurado.")
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    fun getAllReposByUserName() {
        // Realizar a implementacao do callback do retrofit e chamar o metodo setupAdapter se retornar os dados com sucesso
        githubApi.getAllRepositoriesByUser(getSharedPref()!!).enqueue(object : Callback<List<Repository>> {
            override fun onResponse( call: Call<List<Repository>>, response: Response<List<Repository>>){
                if(response.isSuccessful){
                var message = ""
                val repoList= response.body()
                    repoList?.let {
                        setupAdapter(it)
                        repoList.forEach {
                            message += "${it.name} - ${it.htmlUrl}\n"
                        }
                        Log.d(Tag,message)
                    }

                }else{
                    Toast.makeText(applicationContext, R.string.response_error,Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                Toast.makeText(applicationContext, R.string.response_error,Toast.LENGTH_LONG).show()
            }
        })
    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {
        /*
            Implementar a configuracao do Adapter , construir o adapter e instancia-lo
            passando a listagem dos repositorios
         */
        val repAdpter = RepositoryAdapter(list)
        listaRepositories.adapter = repAdpter
        listaRepositories.layoutManager = LinearLayoutManager(this)

        repAdpter.repItemLister = {
            openBrowser(it.htmlUrl)
        }

        repAdpter.btnShareListener = {
            shareRepositoryLink(it.htmlUrl)
        }
    }


    // Metodo responsavel por compartilhar o link do repositorio selecionado
    // Colocar esse metodo no click do share item do adapter
    fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio

    // Colocar esse metodo no click item do adapter
    fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )

    }

    fun saveSharedPref(nome:String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.nome_github),nome)
            apply()
        }
    }

    fun getSharedPref():String? {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getString(getString(R.string.nome_github),"")
    }

}