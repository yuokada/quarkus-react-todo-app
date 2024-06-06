import React, { useState, useEffect } from 'react';

function UserList() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // データを取得する非同期関数を定義
    const fetchData = async () => {
      try {
        const response = await fetch('https://jsonplaceholder.typicode.com/users');
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        const data = await response.json();
        setUsers(data);
        setLoading(false);
      } catch (error) {
        setError(error);
        setLoading(false);
      }
    };

    // データ取得関数を実行
    fetchData();
  }, []); // 空の依存配列は、このエフェクトがマウント時に一度だけ実行されることを示す


  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>Error: {error.message}</div>;
  }

  return (
      <div>
        <h1>User List</h1>
        <ul>
          {users.map(user => (
              <li key={user.id}>
                {user.name} - {user.email}
              </li>
          ))}
        </ul>
      </div>
  );
}

export default UserList;
