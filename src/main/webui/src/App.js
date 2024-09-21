import logo from './logo.svg';
import './App.css';
import {useState} from "react";

const user = {
  name: 'Hedy Lamarr',
  imageUrl: 'https://i.imgur.com/yXOvdOSs.jpg',
  imageSize: 90,
};

const products = [
  {title: 'Cabbage', isFruit: false, id: 1},
  {title: 'Garlic', isFruit: false, id: 2},
  {title: 'Apple', isFruit: true, id: 3},
  {title: 'Apple II', isFruit: true, id: 4},
];

function MyButton({count, onClick}) {
  return (
      <button onClick={onClick}>Clicked {count} times</button>
  );
}

function App() {
  const [count , setCount] = useState(0);
  function handleCLick(){
    setCount(count + 1);
  }

  return (
      <div className="App">
        <MyButton count={count} onClick={handleCLick}/>
        <br/>
        <MyButton count={count} onClick={handleCLick}/>
        <h1>Hello {user.name}!</h1>
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo"/>
          <p>
            Edit <code>src/App.js</code> and save to reload.
          </p>
          <a
              className="App-link"
              href="https://ja.react.dev/"
              target="_blank"
              rel="noopener noreferrer"
          >
            Learn React
          </a>
          <br/>
          <a href={"https://www.yahoo.co.jp"}>Yahoo! Japan</a>
          <br/>
        </header>
      </div>
  );
}

export default App;
